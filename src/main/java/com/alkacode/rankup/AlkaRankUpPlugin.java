package com.alkacode.rankup;

import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.scheduler.AlkaScheduler;
import com.alkacode.rankup.api.AlkaRankUpAPI;
import com.alkacode.rankup.api.AlkaRankUpAPIProvider;
import com.alkacode.rankup.command.PrestigeCommand;
import com.alkacode.rankup.command.RankUpCommand;
import com.alkacode.rankup.command.RankUpTabCompleter;
import com.alkacode.rankup.config.ConfigManager;
import com.alkacode.rankup.config.Messages;
import com.alkacode.rankup.database.HeadsRepository;
import com.alkacode.rankup.database.KitCooldownRepository;
import com.alkacode.rankup.database.PrestigeRewardsRepository;
import com.alkacode.rankup.database.RankDataRepository;
import com.alkacode.rankup.economy.EconomyService;
import com.alkacode.rankup.gui.RankListMenu;
import com.alkacode.rankup.gui.RankUpServices;
import com.alkacode.rankup.hook.AlkaVipsHook;
import com.alkacode.rankup.hook.EconomyHook;
import com.alkacode.rankup.hook.RankUpExpansion;
import com.alkacode.rankup.hook.TimeHook;
import com.alkacode.rankup.listener.HeadDropListener;
import com.alkacode.rankup.listener.PlayerConnectionListener;
import com.alkacode.rankup.manager.HeadsManager;
import com.alkacode.rankup.manager.KitCooldownManager;
import com.alkacode.rankup.manager.PlayerDataManager;
import com.alkacode.rankup.manager.PrestigeManager;
import com.alkacode.rankup.manager.RankManager;
import com.alkacode.rankup.manager.RankUpService;
import com.alkacode.rankup.manager.RequirementChecker;
import com.alkacode.core.plugin.AlkaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.ServicePriority;

import java.util.concurrent.atomic.AtomicReference;

public final class AlkaRankUpPlugin extends AlkaPlugin {

    private PlayerDataManager playerDataManager;
    private RankManager rankManager;

    @Override
    protected void onPluginEnable() {
        ConfigManager configManager = new ConfigManager(this);
        configManager.load();

        AlkaAPI api = getAlkaAPI();
        AlkaScheduler scheduler = api.getScheduler();

        // AlkaEconomy e AlkaVips agora sao softdepend (nao dependem mais de um plugin
        // especifico pra ligar) - resolvidos 1 tick depois do onEnable, quando o servidor
        // garantidamente terminou de habilitar TODOS os plugins. softdepend nao garante
        // ordem estrita com 37+ plugins/dependencias cruzadas (mesma licao ja documentada
        // no AlkaMinesHook/AlkaDropHook - resolver sincrono aqui pode pegar o hook
        // permanentemente vazio se o Economy/Vips habilitar depois do RankUp nesse boot).
        // Guarda um Supplier (nunca o hook resolvido direto) em EconomyService, entao o
        // valor pode passar de null pra pronto sem precisar reconstruir nada.
        AtomicReference<EconomyHook> economyHookRef = new AtomicReference<>(null);
        AtomicReference<AlkaVipsHook> vipsHookRef = new AtomicReference<>(null);
        AtomicReference<TimeHook> timeHookRef = new AtomicReference<>(null);
        Bukkit.getScheduler().runTask(this, () -> {
            economyHookRef.set(EconomyHook.tryHook(getLogger()));
            vipsHookRef.set(AlkaVipsHook.tryHook(getLogger()));
            timeHookRef.set(TimeHook.tryHook(getLogger()));
            if (economyHookRef.get() == null) {
                getLogger().warning("AlkaEconomy nao encontrado (ou nao carregou a API) - "
                        + "rank-up e prestigio ficam indisponiveis ate ele aparecer.");
            }
        });
        com.alkacode.rankup.hook.ItemsAdderHook.tryHook(this);
        EconomyService economyService = new EconomyService(economyHookRef::get);

        RankDataRepository rankDataRepository = new RankDataRepository(api.getDatabase(), getLogger());
        KitCooldownRepository kitCooldownRepository = new KitCooldownRepository(api.getDatabase(), getLogger());
        PrestigeRewardsRepository prestigeRewardsRepository = new PrestigeRewardsRepository(api.getDatabase(), getLogger());
        HeadsRepository headsRepository = new HeadsRepository(api.getDatabase(), getLogger());

        HeadsManager headsManager = new HeadsManager(headsRepository, scheduler);
        RequirementChecker requirementChecker = new RequirementChecker(economyService, timeHookRef::get, headsManager);

        playerDataManager = new PlayerDataManager(rankDataRepository, scheduler);
        KitCooldownManager kitCooldownManager = new KitCooldownManager(kitCooldownRepository, scheduler);
        rankManager = new RankManager(configManager.ranks());
        Messages messages = new Messages(configManager);

        RankUpService rankUpService = new RankUpService(rankManager, playerDataManager, configManager, requirementChecker);
        PrestigeManager prestigeManager = new PrestigeManager(rankManager, playerDataManager, economyService, configManager);

        HeadDropListener headDropListener = new HeadDropListener(this, configManager);
        com.alkacode.core.util.PermissionNamesStore permissionNames =
                new com.alkacode.core.util.PermissionNamesStore(this, "permission-names.yml");

        RankUpServices services = new RankUpServices(this, rankManager, playerDataManager, kitCooldownManager,
                headsManager, rankUpService, prestigeManager, requirementChecker, configManager, messages,
                headDropListener.headIdKey(), timeHookRef::get, prestigeRewardsRepository, permissionNames,
                new com.alkacode.rankup.gui.layout.GuiLayoutLoader(this));

        getCommand("rankup").setExecutor(new RankUpCommand(services));
        getCommand("rankup").setTabCompleter(new RankUpTabCompleter(rankManager));
        getCommand("prestige").setExecutor(new PrestigeCommand(services));

        PlayerConnectionListener connectionListener = new PlayerConnectionListener(playerDataManager, kitCooldownManager, headsManager);
        getServer().getPluginManager().registerEvents(connectionListener, this);
        getServer().getPluginManager().registerEvents(headDropListener, this);
        // Nenhum listener de clique de GUI proprio: todas as telas agora extendem BaseGui
        // e sao roteadas pelo GuiListener unico registrado pelo AlkaCore (regra R1).

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new RankUpExpansion(this, rankManager, playerDataManager, configManager, kitCooldownManager, requirementChecker).register();
            getLogger().info("Hook do PlaceholderAPI registrado.");
        }

        // cobre o caso de /reload: jogadores ja online nao disparam PlayerJoinEvent de novo.
        for (var player : Bukkit.getOnlinePlayers()) {
            connectionListener.handle(player);
        }

        AlkaRankUpAPI rankUpApi = new AlkaRankUpAPIProvider(playerDataManager, configManager);
        getServer().getServicesManager().register(AlkaRankUpAPI.class, rankUpApi, this, ServicePriority.Normal);

        int rankCount = rankManager.ranks().size();
        if (rankCount > RankListMenu.MAX_RANKS) {
            getLogger().warning("Existem " + rankCount + " rank(s) configurado(s), mas a RankListMenu so tem "
                    + RankListMenu.MAX_RANKS + " slot(s) - os ranks excedentes nao vao aparecer na GUI.");
        }

        getLogger().info("AlkaRankUp habilitado com " + rankManager.ranks().size() + " ranks.");
    }

    @Override
    protected void onPluginDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllSync();
        }
        // conexao/pool e do AlkaCore - fechada pelo proprio Core no seu onDisable, nunca aqui.
    }

    /** API publica minima para outros plugins Alka* (ex: AlkaMines) consultarem ranks. */
    public RankManager getRankManager() {
        return rankManager;
    }

    /** API publica minima para outros plugins Alka* consultarem o rank atual de um jogador. */
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}
