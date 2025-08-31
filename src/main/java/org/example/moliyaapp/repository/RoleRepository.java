package org.example.moliyaapp.repository;


import org.example.moliyaapp.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<UserRole, Long> {

    Optional<UserRole> findByNameAndDeletedFalse(String name);

    @Query(value = "select r.* from user_roles as ur\n" +
            "inner join roles as r on r.id=ur.role_id\n" +
            "where ur.user_id=?1", nativeQuery = true)
    List<UserRole> findByUserId(Long userId);

    Set<UserRole> findAllByNameIn(@Param("roleNames") Set<String> roleNames);

    @Query("select count(ur)>0 from roles as ur where ur.id=:roleId")
    boolean existsByRoleId(@Param("roleId") Long roleId);

    @Transactional
    @Modifying
    @Query(value = "delete from user_roles where user_id=?1", nativeQuery = true)
    void deleteUserRoleByUserId(Long userId);

    boolean existsByNameAndDeletedFalse(@Param("name") String name);
}
