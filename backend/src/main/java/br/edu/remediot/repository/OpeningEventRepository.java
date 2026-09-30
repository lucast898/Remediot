package br.edu.remediot.repository;

import br.edu.remediot.entity.OpeningEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OpeningEventRepository extends JpaRepository<OpeningEvent, Long> {

    List<OpeningEvent> findByDoseId(Long doseId);

    void deleteByDoseIdIn(Collection<Long> doseIds);
}
