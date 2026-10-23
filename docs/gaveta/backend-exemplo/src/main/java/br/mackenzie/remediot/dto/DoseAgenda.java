package br.mackenzie.remediot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Um remédio num horário (equivale a um MedicationSchedule do app).
 * Vários remédios no mesmo horário são várias entradas com a mesma hora.
 *
 * @param doseId     1–32 caracteres [A-Za-z0-9_-], único na agenda. Sugestão: "s" + id do schedule
 * @param hora       "HH:mm" no horário de São Paulo
 * @param diasSemana opcional; null = todos os dias (como no app)
 * @param peso       1, 2 ou 3 (peso do remédio); null = 2. Peso 2 e 3 têm alarme sonoro no atraso
 * @param descricao  opcional; o ESP guarda só os 32 primeiros caracteres. Não usar nome de remédio (LGPD)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DoseAgenda(String doseId, String hora, List<DiaSemana> diasSemana, Integer peso, String descricao) {
}
