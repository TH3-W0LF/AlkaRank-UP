# AlkaRankUp

Rank-up com progressão exponencial, prestígio e minério "cabeças" para a rede
AlkaStudio (Paper 1.21.8 / Java 21) — construído sobre o AlkaCore.

## O que faz

- **Ranks com requisitos multi-tipo** — cada rank exige uma combinação de
  moedas do AlkaEconomy (`coins`, `escarion`, etc), tempo online (via
  AlkaTime) e/ou `head:<id>` (ver Cabeças abaixo), todos precisam estar
  satisfeitos ao mesmo tempo. Motor único (`RequirementChecker`) usado tanto
  pra checar/cobrar quanto pra exibir o que falta na GUI.
- **Prestígio** — reseta o rank pro inicial em troca de uma vantagem
  permanente: troca de grupo do LuckPerms (`lp-group` por rank), kit por
  rank (diário/semanal/mensal, cooldown próprio), recompensas ÚNICAS por
  nível de prestígio (resgatáveis uma vez só), `/prestige fly` liberado a
  partir de um nível configurável, e bônus de venda (`sell-bonus-per-prestige`)
  consumido pelo AlkaShop via `AlkaRankUpAPI`.
- **Cabeças** — matar um mob configurado (`heads.types.*`) tem chance de
  dropar uma cabeça física (material vanilla quando existe — Zumbi/
  Esqueleto/Esqueleto Wither/Creeper — ou `PLAYER_HEAD` com textura Base64
  configurada pelo admin pros demais mobs). O jogador deposita no menu
  Cabeças ("Depositar Tudo", varre o inventário pela tag do drop); o saldo
  bancado vira um requisito de rankup normal (`"head:zombie": 50`). Pensado
  pra integrar com um futuro plugin de spawners.
- **GUI** — hub principal (`RankMainMenu`, 6 botões: Ranks/Perfil/Rankup/
  Cabeças/Renascimento/Top) + submenus (lista de ranks, confirmação de
  rankup/prestígio, perfil, cabeças, kits, top paginado, recompensas de
  prestígio) — tudo `extends BaseGui`, sem listener de clique próprio (o
  `GuiListener` único do AlkaCore cuida de tudo).
- **API pública** (`AlkaRankUpAPI`, via ServicesManager) — `getSellMultiplier(UUID)`
  pro AlkaShop aplicar o bônus de prestígio na venda.
- **PlaceholderAPI** — rank/prestígio/progresso/cooldown de kit, ver
  `hook/RankUpExpansion`.

## Dependências

- **AlkaCore** (hard dependency) — GUI (`BaseGui`/`GuiListener`), banco
  (`AC#getDatabase()` via `AbstractRepository`), scheduler assíncrono.
- **AlkaEconomy** — softdepend + hook por reflection (`hook/EconomyHook`,
  nunca `import com.alkacode.economy.*` direto).
- **AlkaVips**, **AlkaTime** — softdepend + reflection, mesma convenção
  (`AlkaVipsHook`, `TimeHook`) — sem eles, os recursos que dependem
  degradam (ex: `online_time` nunca satisfeito, benefícios de VIP não
  aplicados) em vez de o plugin desativar.
- **PlaceholderAPI** — softdepend direto (compileOnly).

## Banco de dados

Zero JDBC próprio — 4 repositórios (`database/`) sobre o
`DatabaseProvider` do AlkaCore, mesmo padrão do `AlkaTime.TimeRepository`
(`CREATE TABLE IF NOT EXISTS` idempotente + `AbstractRepository#upsert`):
`rankup_players` (rank/prestígio), `alka_kits_cooldowns`,
`rankup_prestige_rewards_claimed`, `rankup_heads` (banco de cabeças).

## Limitações conhecidas (v1.0.16)

- **Texturas de cabeça**: só os 4 mobs com cabeça vanilla de verdade
  (Zumbi/Esqueleto/Esqueleto Wither/Creeper) vêm prontos. Qualquer outro
  mob precisa que o admin cole um valor Base64 real em `heads.types.<MOB>.texture`
  no `config.yml` — nenhum valor foi inventado.
- **Depósito de cabeça é um botão, não drag-and-drop**: o `GuiListener`
  compartilhado do AlkaCore cancela incondicionalmente qualquer clique/drag
  que toque o inventário do jogador enquanto uma `BaseGui` está aberta, então
  "arrastar a cabeça pro menu" não é viável sem mudar código compartilhado do
  Core — o `HeadsMenu` varre o inventário via um botão "Depositar Tudo".
- **Kits não foram extraídos pra um plugin separado** — cogitado numa rodada
  anterior de melhorias, mas ficaram dentro do AlkaRankUp mesmo na migração
  completa pro AlkaCore.
- Não testado em servidor real ainda — build local (`./gradlew build`)
  verificado limpo, mas fluxo de jogo/GUI precisa de teste manual.

## Origem

Migrado de uma arquitetura 100% própria (SQLite via JDBC cru + GUIs com
`InventoryHolder`/`Listener` individuais por menu) pro AlkaCore numa única
tarefa: DB, GUI e o sistema de Cabeças (pedido durante o desenho da nova UI,
a partir de uma especificação externa que citava um menu "Heads" sem
contexto do projeto — investigado com o usuário e descoberto ser, de fato,
uma feature nova de drop-por-mob, não um erro de tradução do menu de Kits).
