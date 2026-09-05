package com.zb.jogakjogak.jobdescription.repository;


import com.zb.jogakjogak.jobdescription.entity.JD;
import com.zb.jogakjogak.jobdescription.entity.ToDoList;
import com.zb.jogakjogak.jobdescription.type.ToDoListType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToDoListRepository extends JpaRepository<ToDoList, Long>, ToDoListRepositoryCustom {

    //테스트용
    List<ToDoList> findAllByJdId(Long jdId);
    /*
    @Query("SELECT COUNT(t) FROM toDoList t JOIN t.jd j WHERE t.isDone = true AND j.Id = :jdId")
    Integer countByIsDoneTrueAndJd_JdId(@Param("jdId") Long jdId);
    */
    Integer countByIsDoneTrueAndJd_Id(Long jdId);

    Integer countByJd_Id(Long id);
}
