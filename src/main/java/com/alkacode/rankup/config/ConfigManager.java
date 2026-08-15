package com.alkacode.rankup.config;

import com.alkacode.rankup.model.HeadType;
import com.alkacode.rankup.model.IconConfig;
import com.alkacode.rankup.model.Kit;
import com.alkacode.rankup.model.KitItem;
import com.alkacode.rankup.model.KitType;
import com.alkacode.rankup.model.Rank;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class ConfigManager {

    private final JavaPlugin plugin;

    private FileConfiguration config;
    private FileConfiguration messages;

    private List<Rank> ranks;
    private double prestigeBaseCost;
    private double prestigeCostGrowth;
    private String defaultCurrency;
    private Map<Integer, List<KitItem>> prestigeRewards;
    private Map<String, HeadType> headTypes;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        // getConfig() sozinho NAO mescla chave nova do jar (ex: "economy.currency"
        // adicionada num update) num config.yml que ja existe em disco - so cria o
        // arquivo se ele nao existir. Sem isso, quem ja tem o plugin instalado nunca
        // ganha a chave nova sem apagar o arquivo na mao (mesmo bug ja corrigido no
        // AlkaShop). copyDefaults + save aplica so o que falta, nunca sobrescreve o
        // que o admin ja customizou.
        config.setDefaults(YamlConfiguration.loadConfiguration(
                new InputStreamReader(java.util.Objects.requireNonNull(plugin.getResource("config.yml")), StandardCharsets.UTF_8)));
        config.options().copyDefaults(true);
        plugin.saveConfig();
        messages = loadMessages();

        this.prestigeCostGrowth = config.getDouble("economy.prestige-cost-growth", 1.25);
        this.prestigeBaseCost = config.getDouble("economy.prestige-base-cost", 0.0);
        this.defaultCurrency = config.getString("economy.currency", "coins");

        this.ranks = parseRanks();
        this.prestigeRewards = parsePrestigeRewards();
        this.headTypes = parseHeads();
    }

    public void reload() {
        plugin.reloadConfig();
        load();
    }

    private FileConfiguration loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        try (InputStream defaultStream = plugin.getResource("messages.yml")) {
            if (defaultStream != null) {
                loaded.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8)));
                loaded.options().copyDefaults(true);
                loaded.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao carregar defaults de messages.yml", e);
        }
        return loaded;
    }

    /**
     * A ordem dos ranks e definida pela ordem em que suas chaves aparecem no
     * config.yml (o id do rank e a propria chave do mapa) - o YAML mantem a ordem de
     * insercao, entao nao ha necessidade de uma chave numerica separada.
     */
    private List<Rank> parseRanks() {
        List<Rank> parsed = new ArrayList<>();
        ConfigurationSection ranksSection = config.getConfigurationSection("ranks");
        if (ranksSection == null) {
            plugin.getLogger().warning("Nenhum rank configurado em config.yml!");
            return parsed;
        }

        int index = 0;
        for (String id : ranksSection.getKeys(false)) {
            ConfigurationSection rankSection = ranksSection.getConfigurationSection(id);
            if (rankSection == null) {
                continue;
            }

            String displayName = rankSection.getString("display_name", id);
            IconConfig icon = parseIcon(rankSection.getConfigurationSection("icon"), Material.STONE);
            Map<String, Double> requirements = parseRequirements(rankSection);
            String lpGroup = rankSection.getString("lp-group", id);
            Map<KitType, Kit> kits = parseKits(rankSection.getConfigurationSection("kits"));

            parsed.add(new Rank(index, id, displayName, icon, requirements, lpGroup, kits));
            index++;
        }
        return parsed;
    }

    /**
     * Recompensa UNICA por nivel de prestigio (ver PrestigeRewardsMenu) - secao
     * `prestige-rewards`, chave = nivel (string numerica), valor = lista de itens no
     * MESMO formato dos itens de kit (reaproveita parseKitItems). So resgatavel uma
     * vez por nivel, ao contrario dos kits de rank que sao recorrentes.
     */
    private Map<Integer, List<KitItem>> parsePrestigeRewards() {
        Map<Integer, List<KitItem>> rewards = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("prestige-rewards");
        if (section == null) {
            return rewards;
        }
        for (String key : section.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                rewards.put(level, parseKitItems(section.getMapList(key)));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("prestige-rewards: nivel invalido '" + key + "' (precisa ser numero).");
            }
        }
        return rewards;
    }

    /**
     * heads.types.&lt;ENTITY_TYPE&gt; -&gt; HeadType (drop de cabeca ao matar o mob, ver
     * HeadDropListener). `material` e usado quando o mob tem cabeca vanilla de verdade
     * (ZOMBIE_HEAD/SKELETON_HEAD/WITHER_SKELETON_SKULL/CREEPER_HEAD/PLAYER_HEAD); qualquer
     * outro mob precisa de `texture` (Base64) preenchido a mao - sem os dois, cai no
     * PLAYER_HEAD generico (HeadType#hasTexture() == false e material == PLAYER_HEAD).
     */
    private Map<String, HeadType> parseHeads() {
        Map<String, HeadType> types = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("heads.types");
        if (section == null) {
            return types;
        }
        for (String entityKey : section.getKeys(false)) {
            ConfigurationSection headSection = section.getConfigurationSection(entityKey);
            if (headSection == null) {
                continue;
            }
            EntityType entityType;
            try {
                entityType = EntityType.valueOf(entityKey.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("heads.types: tipo de mob invalido '" + entityKey + "'.");
                continue;
            }

            String id = headSection.getString("id", entityKey.toLowerCase());
            String displayName = headSection.getString("display-name", id);
            String texture = headSection.getString("texture", "");
            Material material = Material.matchMaterial(headSection.getString("material", "PLAYER_HEAD"));
            if (material == null) {
                material = Material.PLAYER_HEAD;
            }
            double chance = headSection.getDouble("chance", 0.0);

            if (texture.isBlank() && material == Material.PLAYER_HEAD) {
                plugin.getLogger().warning("heads.types." + entityKey + ": sem 'material' vanilla nem 'texture' Base64 - "
                        + "vai usar PLAYER_HEAD generico ate voce configurar um dos dois.");
            }

            types.put(id, new HeadType(id, entityType, displayName, material, texture, chance));
        }
        return types;
    }

    /**
     * Formato novo: secao `requirements` com uma chave por moeda (ou "online_time",
     * em segundos) e o valor necessario - ex: {@code requirements: {coins: 30000000,
     * escarion: 20000, online_time: 288000}}. Formato antigo (`cost`+`currency`, um so
     * requisito) continua funcionando como fallback pra nao quebrar config existente.
     */
    private Map<String, Double> parseRequirements(ConfigurationSection rankSection) {
        ConfigurationSection reqSection = rankSection.getConfigurationSection("requirements");
        if (reqSection != null) {
            Map<String, Double> requirements = new LinkedHashMap<>();
            for (String key : reqSection.getKeys(false)) {
                requirements.put(key.toLowerCase(), reqSection.getDouble(key, 0.0));
            }
            return requirements;
        }

        double cost = rankSection.getDouble("cost", 0.0);
        if (cost <= 0) {
            return Map.of();
        }
        String currency = rankSection.getString("currency", defaultCurrency);
        Map<String, Double> requirements = new LinkedHashMap<>();
        requirements.put(currency.toLowerCase(), cost);
        return requirements;
    }

    private Map<KitType, Kit> parseKits(ConfigurationSection kitsSection) {
        Map<KitType, Kit> kits = new EnumMap<>(KitType.class);
        if (kitsSection == null) {
            return kits;
        }

        for (KitType type : KitType.values()) {
            ConfigurationSection kitSection = kitsSection.getConfigurationSection(type.name().toLowerCase());
            if (kitSection == null) {
                continue;
            }

            String name = kitSection.getString("name", type.name());
            long cooldownSeconds = kitSection.getLong("cooldown", 0);
            IconConfig iconAvailable = parseIcon(kitSection.getConfigurationSection("icon_available"), Material.CHEST);
            IconConfig iconCooldown = parseIcon(kitSection.getConfigurationSection("icon_cooldown"), Material.GRAY_STAINED_GLASS_PANE);
            List<KitItem> items = parseKitItems(kitSection.getMapList("items"));

            kits.put(type, new Kit(type, name, cooldownSeconds, iconAvailable, iconCooldown, items));
        }
        return kits;
    }

    private List<KitItem> parseKitItems(List<Map<?, ?>> rawItems) {
        List<KitItem> items = new ArrayList<>();
        for (Map<?, ?> raw : rawItems) {
            Material material = Material.matchMaterial(String.valueOf(raw.get("material")));
            if (material == null) {
                plugin.getLogger().warning("Item de kit com material invalido: " + raw.get("material"));
                continue;
            }

            int amount = raw.get("amount") instanceof Number number ? number.intValue() : 1;
            String displayName = raw.get("name") != null ? String.valueOf(raw.get("name")) : null;

            List<String> lore = raw.get("lore") instanceof List<?> rawLore
                    ? rawLore.stream().map(String::valueOf).toList()
                    : List.of();

            int customModelData = raw.get("custom_model_data") instanceof Number number ? number.intValue() : 0;
            Map<Enchantment, Integer> enchantments = parseEnchantments(raw.get("enchantments"));

            items.add(new KitItem(material, amount, displayName, lore, customModelData, enchantments));
        }
        return items;
    }

    /**
     * Le um objeto de icone no formato {@code {material, custom_model_data, name}}.
     * `name` e opcional - fica nulo quando o icone nao carrega nome proprio (ex: icone
     * de rank, cujo nome ja vem de um campo separado).
     */
    /**
     * {@code texture} (Base64) e {@code itemsadder} (namespace, ex: "alkarankup:rank_1")
     * sao opcionais - quando presentes tem prioridade sobre {@code material} na hora de
     * montar o ItemStack (ver IconFactory#buildBase). Nao exigem plugin nenhum instalado
     * pra CONFIGURAR (so pra RENDERIZAR: sem ItemsAdder instalado, itemsadder cai pro
     * material vanilla; texture sempre funciona, e so um PLAYER_HEAD com skin custom).
     */
    private IconConfig parseIcon(ConfigurationSection section, Material fallbackMaterial) {
        if (section == null) {
            return new IconConfig(fallbackMaterial, 0, null);
        }
        Material material = Material.matchMaterial(section.getString("material", fallbackMaterial.name()));
        if (material == null) {
            material = fallbackMaterial;
        }
        int customModelData = section.getInt("custom_model_data", 0);
        String name = section.getString("name", null);
        String texture = section.getString("texture", null);
        String itemsAdderId = section.getString("itemsadder", null);
        return new IconConfig(material, customModelData, name, texture, itemsAdderId);
    }

    private Map<Enchantment, Integer> parseEnchantments(Object rawEnchantments) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        if (!(rawEnchantments instanceof Map<?, ?> map)) {
            return enchantments;
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey()).toLowerCase();
            Enchantment enchantment = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .get(NamespacedKey.minecraft(key));
            if (enchantment == null) {
                plugin.getLogger().warning("Encantamento desconhecido no config.yml: " + key);
                continue;
            }
            int level = entry.getValue() instanceof Number number ? number.intValue() : 1;
            enchantments.put(enchantment, level);
        }
        return enchantments;
    }

    public List<Rank> ranks() {
        return ranks;
    }

    public double prestigeBaseCost() {
        return prestigeBaseCost;
    }

    public double prestigeCostGrowth() {
        return prestigeCostGrowth;
    }

    /** Moeda usada quando um rank nao especifica a sua propria, e pelo custo de /prestige. */
    public String defaultCurrency() {
        return defaultCurrency;
    }

    /** Se true, ranquear troca o grupo do LuckPerms (Rank#lpGroup) - remove o do rank anterior, adiciona o do novo. */
    public boolean luckPermsSyncEnabled() {
        return config.getBoolean("luckperms.sync-groups", true);
    }

    /** Itens de recompensa unica do nivel de prestigio, ou lista vazia se o nivel nao tiver recompensa configurada. */
    public List<KitItem> prestigeRewardItems(int prestigeLevel) {
        return prestigeRewards.getOrDefault(prestigeLevel, List.of());
    }

    /** Todo nivel de prestigio que tem recompensa configurada, em ordem crescente. */
    public java.util.List<Integer> prestigeRewardLevels() {
        return prestigeRewards.keySet().stream().sorted().toList();
    }

    public boolean headsEnabled() {
        return config.getBoolean("heads.enabled", true);
    }

    public boolean headsGiveDirectlyToInventory() {
        return config.getBoolean("heads.give-directly-to-inventory", true);
    }

    /** Todos os tipos de cabeca configurados, por id (nao pelo nome do EntityType). */
    public java.util.Collection<HeadType> headTypes() {
        return headTypes.values();
    }

    public HeadType headType(String id) {
        return headTypes.get(id);
    }

    /** null se o mob nao tiver cabeca configurada em heads.types. */
    public HeadType headTypeForEntity(EntityType entityType) {
        for (HeadType type : headTypes.values()) {
            if (type.entityType() == entityType) {
                return type;
            }
        }
        return null;
    }

    /** 0 = /fly desativado pra todo mundo. N = liberado a partir do Prestigio N (inclusive). */
    public int flyFromPrestige() {
        return config.getInt("perks.fly-from-prestige", 0);
    }

    /** % de bonus na venda (AlkaShop) por nivel de prestigio - ex: 0.02 = +2% por prestigio. 0 = sem bonus. */
    public double sellBonusPerPrestige() {
        return config.getDouble("perks.sell-bonus-per-prestige", 0.0);
    }

    public List<String> prestigeCommands() {
        return config.getStringList("prestige.commands");
    }

    /** placeholders: %player% e %rank% */
    public List<String> rankupCommands() {
        return config.getStringList("rankup.commands");
    }

    public boolean broadcastRankup() {
        return config.getBoolean("broadcast.rankup", true);
    }

    public boolean broadcastPrestige() {
        return config.getBoolean("broadcast.prestige", true);
    }

    /** feedback.rankup ou feedback.prestige - som/particula/title do sucesso (ver FeedbackUtil). */
    public ConfigurationSection feedbackSection(String event) {
        return config.getConfigurationSection("feedback." + event);
    }

    public ConfigurationSection guiSection() {
        return config.getConfigurationSection("gui");
    }

    public ConfigurationSection kitsMenuSection() {
        return config.getConfigurationSection("gui.kits-menu");
    }

    public ConfigurationSection kitPreviewSection() {
        return config.getConfigurationSection("gui.kit-preview");
    }

    public ConfigurationSection prestigeConfirmSection() {
        return config.getConfigurationSection("gui.prestige-confirm");
    }

    public boolean closeGuiOnClaim() {
        return config.getBoolean("gui_settings.close_gui_on_claim", true);
    }

    public boolean fillEmptySlotsEnabled() {
        return config.getBoolean("gui_settings.fill_empty_slots.enabled", true);
    }

    public IconConfig fillEmptySlotsIcon() {
        return parseIcon(config.getConfigurationSection("gui_settings.fill_empty_slots"), Material.BLACK_STAINED_GLASS_PANE);
    }

    public IconConfig backButtonIcon() {
        return parseIcon(config.getConfigurationSection("gui_settings.back_button"), Material.ARROW);
    }

    public int backButtonSlot() {
        return config.getInt("gui_settings.back_button.slot", -1);
    }

    public IconConfig claimButtonIcon() {
        return parseIcon(config.getConfigurationSection("gui_settings.claim_button"), Material.EMERALD_BLOCK);
    }

    public int claimButtonSlot() {
        return config.getInt("gui_settings.claim_button.slot", -1);
    }

    public IconConfig cooldownButtonIcon() {
        return parseIcon(config.getConfigurationSection("gui_settings.cooldown_button"), Material.BARRIER);
    }

    public FileConfiguration messages() {
        return messages;
    }

    /** Formato de cada linha de "permissoes deste rank" (marcador "<permissions>" na lore -
     * ver RankUpServices#permissionLoreLines/PermissionLoreUtil no AlkaCore). */
    public String permissionLoreLineFormat() {
        return config.getString("permission-lore.line-format", " <gray>- <white>%permission%");
    }

    public String rawMessage(String path) {
        String value = messages.getString(path);
        return value == null ? path : value;
    }

    public List<String> rawMessageList(String path) {
        return messages.getStringList(path);
    }

    public String prefix() {
        return rawMessage("prefix");
    }
}
