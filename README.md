<div align="center">

# AlkaRankUp

### Rank-up com progressão exponencial e prestígio

Ranks com requisitos multi-tipo, prestígio com vantagens permanentes e um
sistema de "cabeças" colecionáveis — construído sobre o AlkaCore, para a
rede AlkaStudio.

![Java](https://img.shields.io/badge/Java-21-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Version](https://img.shields.io/badge/Version-1.0.19-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

</div>

---

## 📋 Sobre o Projeto

O **AlkaRankUp** é o sistema de progressão por ranks da rede AlkaStudio. Cada
rank pode exigir uma combinação de moedas, tempo online e itens colecionáveis
ao mesmo tempo, e ao chegar no topo o jogador pode prestigiar — resetando o
progresso em troca de vantagens permanentes que acompanham o jogador para
sempre.

## ✨ Funcionalidades Principais

| Módulo | Descrição |
| --- | --- |
| 🪜 **Ranks multi-requisito** | Cada rank pode exigir moedas do AlkaEconomy, tempo online (via AlkaTime) e/ou cabeças colecionáveis, tudo checado por um motor único. |
| 👑 **Prestígio** | Reseta o rank para o inicial em troca de grupo de permissão exclusivo, kits periódicos, recompensas únicas por nível, fly liberado e bônus de venda. |
| 💀 **Cabeças** | Mobs configurados podem dropar uma cabeça colecionável ao morrer; o jogador deposita no banco de cabeças, que vira requisito de rankup. |
| 🖼️ **GUI completa** | Hub principal com acesso a ranks, perfil, rankup, cabeças, prestígio e ranking — tudo em menus nativos do AlkaCore. |
| 🏆 **Ranking** | Top de jogadores por progresso, exibido em GUI paginada. |
| 🔌 **API pública** | `AlkaRankUpAPI` expõe o multiplicador de venda por prestígio para outros plugins (ex.: AlkaShop). |
| 🔤 **PlaceholderAPI** | Placeholders de rank, prestígio, progresso e cooldown de kit. |

## 🎮 Comandos

| Comando | Descrição | Permissão |
| --- | --- | --- |
| `/rankup [kits <rank_id>\|top [quantidade]]` | Abre o menu de rank-up, kits por rank e o ranking | `alkarankup.use` |
| `/prestige [rewards\|fly]` | Reinicia o progresso no rank máximo por uma vantagem permanente | `alkarankup.use` |
| `/rankup admin <setrank\|setprestige\|reload>` | Comandos administrativos | `alkarankup.admin` |

Aliases: `/ranks`, `/rank`, `/evoluir` (para `/rankup`).

## 🔗 Integrações

- **AlkaCore** (obrigatória) — GUI, banco de dados e scheduler assíncrono.
- **AlkaEconomy** — moedas usadas nos requisitos de rank.
- **AlkaVips** — benefícios de VIP aplicados na progressão.
- **AlkaTime** — requisito de tempo online.
- **AlkaShop** — consome o bônus de venda por prestígio via `AlkaRankUpAPI`.
- **LuckPerms** — troca de grupo por rank/prestígio.
- **PlaceholderAPI**, **ItemsAdder** — placeholders e ícones de rank.

## 🔧 Tecnologias Utilizadas

- **Java 21** · **Gradle** (com `shadow`)
- **Paper API 1.21.8**
- **Adventure/MiniMessage** para mensagens e GUI
- Repositórios próprios sobre o `DatabaseProvider` do AlkaCore

## ⚙️ Instalação

1. Instale o **AlkaCore** (e o **AlkaEconomy**, se quiser usar moedas nos
   requisitos) primeiro.
2. Coloque `AlkaRankUp.jar` na pasta `plugins/` do servidor (Paper **1.21.8+**).
3. Reinicie o servidor.
4. Configure ranks, prestígio e cabeças em `config.yml`.

## 🔐 Permissões

| Permissão | Padrão | Descrição |
| --- | --- | --- |
| `alkarankup.use` | true | Permite usar `/rankup` e `/prestige` |
| `alkarankup.admin` | op | Permite comandos administrativos do AlkaRankUp |

## 📝 Licença

> ⚠️ **Projeto proprietário da AlkaStudio.**
>
> Código fonte destinado exclusivamente ao uso interno da rede `Alka*`.
> Reprodução, distribuição ou uso não autorizado não são permitidos.

## 🎯 Créditos

- **Desenvolvido por**: MestreDEV — AlkaStudio
- **Parte de**: todo o ecossistema `Alka*`

---

<div align="center">

**Desenvolvido com ❤️ pela AlkaStudio**

[![AlkaStudio](https://img.shields.io/badge/AlkaStudio-JLob0-blue)](https://github.com/JLob0)

</div>
