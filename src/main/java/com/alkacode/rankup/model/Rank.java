package com.alkacode.rankup.model;

import java.util.Map;
import java.util.Optional;

/**
 * `requirements`: chave -> quantidade necessaria pra ranquear. Cada chave e um id de
 * moeda valido do AlkaEconomy (ex: "coins", "escarion"), OU a chave especial
 * "online_time" (em segundos, checada via AlkaTime - ver RequirementChecker). LinkedHashMap
 * na ordem do config.yml, pra exibir na mesma ordem que o admin escreveu.
 *
 * `lpGroup`: grupo do LuckPerms aplicado ao ranquear pra este rank (troca o do rank
 * anterior, nunca acumula) - default e o proprio id do rank se nao especificado.
 */
public record Rank(int index, String id, String displayName, IconConfig icon,
                    Map<String, Double> requirements, String lpGroup, Map<KitType, Kit> kits) {

    public Optional<Kit> kit(KitType type) {
        return Optional.ofNullable(kits.get(type));
    }
}
