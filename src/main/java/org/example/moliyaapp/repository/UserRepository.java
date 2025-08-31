package org.example.moliyaapp.repository;

import jakarta.transaction.Transactional;
import org.example.moliyaapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByPhoneNumberAndDeletedFalse(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndDeletedFalse(Long employeeId);

    boolean existsByContractNumberAndIdNot(String contractNo, Long id);

    boolean existsByContractNumber(String contractNumber);

    @Query(value = "select * from users as u where u.status='ACTIVE'", nativeQuery = true)
    Page<User> getAllActiveUsers(Pageable pageable);

    @Query(value = "select * from users as u where u.status='ACTIVE'", nativeQuery = true)
    List<User> getAllActiveUsers();

    @Query("select u from User u where u.id = :id and (u.deleted = false or u.deleted is null)")
    Optional<User> findUserById(@Param("id") Long id);

    @Query(value = "select * from users as u where u.usernames ilike ?1", nativeQuery = true)
    Optional<User> checkUsername(String username);

    @Modifying
    @Transactional
    @Query(value = "delete from user_roles as us where us.user_id=?1 and us.role_id=?2", nativeQuery = true)
    void deleteRoleFromUser(Long userId, Long roleId);


    @Query(value = "select u.* from users as u \n" +
            "inner join user_roles as ur on u.id=ur.user_id\n" +
            "inner join roles as r on r.id=ur.role_id\n" +
            "where r.name=?1", nativeQuery = true)
    List<User> getAllUserByRole(String role);

    @Query(value = "select u.* from users as u \n" +
            "inner join user_roles as ur on u.id=ur.user_id\n" +
            "where ur.role_id=?1 and u.deleted is false", nativeQuery = true)
    Page<User> getAllActiveByRoleId(Long roleId, Pageable pageable);

    @Query("select u from User as u where u.deleted=false")
    Page<User> getAllInActive(Pageable pageable);

    @Query("select u from User as u join u.role as r where r.name=:role and u.deleted=false")
    Page<User> getAllActive(@Param("role") String role, Pageable pageable);

    @Query("""
                SELECT u FROM User u
                WHERE u.deleted = false AND u.status = 'ACTIVE'
                  AND (
                      LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                      u.contractNumber LIKE CONCAT('%', :keyword, '%') OR
                      u.phoneNumber LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<User> searchEmployee(@Param("keyword") String keyword, Pageable pageable);

    @Query("select u.id from User as u where u.deleted=true and u.id in :ids")
    List<Long> findAllById(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("delete from User as u where u.deleted=true and u.id in :ids")
    void deleteAllById(List<Long> ids);

    @Query("select case when count(u) > 0 then true else false end from User u " +
            "join u.role r " +
            "where r.name = :name and u.deleted = false and u.id <> :userId")
    boolean existsByNameAndDeletedFalse(@Param("name") String name, @Param("userId") Long userId);

    @Query("select u from User u where u.id in :employeeIds and u.deleted=false")
    List<User> findAllByIdAndDeletedFalse(@Param("employeeIds") List<Long> employeeIds);

    Optional<User> findByFullName(String employeeName);

    boolean existsByPhoneNumberAndDeletedFalse(String phone);
}
