package com.printnow.module.parametres.repository;

import com.printnow.module.parametres.model.ParametresPlateforme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametresPlateformeRepository extends JpaRepository<ParametresPlateforme, Long> {
}
