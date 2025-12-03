package com.ross.theovalguide.repo;

import com.ross.theovalguide.model.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfessorRepository extends JpaRepository<Professor, UUID> {

        Optional<Professor> findBySlugIgnoreCase(String slug);

        boolean existsBySlugIgnoreCase(String slug);

        java.util.List<Professor> findBySlugIsNull();

        @org.springframework.data.jpa.repository.Query("""
                          select p from Professor p
                          where upper(p.name) like upper(concat('%', :q, '%'))
                             or upper(p.department) like upper(concat('%', :q, '%'))
                             or upper(p.university) like upper(concat('%', :q, '%'))
                             or upper(p.slug) like upper(concat('%', :slug, '%'))
                        """)
        org.springframework.data.domain.Page<Professor> searchProfessors(
                        @org.springframework.data.repository.query.Param("q") String q,
                        @org.springframework.data.repository.query.Param("slug") String slug,
                        org.springframework.data.domain.Pageable pageable);

}