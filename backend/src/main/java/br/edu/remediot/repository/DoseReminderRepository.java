package br.edu.remediot.repository;

import br.edu.remediot.entity.DoseReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoseReminderRepository extends JpaRepository<DoseReminder, Long> {

    List<DoseReminder> findByDoseId(Long doseId);
}
