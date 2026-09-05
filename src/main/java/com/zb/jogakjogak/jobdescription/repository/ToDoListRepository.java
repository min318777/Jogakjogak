package com.zb.jogakjogak.jobdescription.repository;


import com.zb.jogakjogak.jobdescription.entity.ToDoList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToDoListRepository extends JpaRepository<ToDoList, Long>, ToDoListRepositoryCustom {

    Integer countByIsDoneTrueAndJd_Id(Long jdId);

    Integer countByJd_Id(Long id);
}
