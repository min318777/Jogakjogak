package com.zb.jogakjogak.jobdescription.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zb.jogakjogak.jobdescription.entity.QJD;
import com.zb.jogakjogak.jobdescription.entity.QToDoList;
import com.zb.jogakjogak.jobdescription.entity.ToDoList;
import com.zb.jogakjogak.jobdescription.type.ToDoListType;

import java.util.List;
import java.util.Optional;

public class ToDoListRepositoryImpl implements ToDoListRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    public ToDoListRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Optional<ToDoList> findToDoListWithJdByIdAndJdId(Long toDoListId, Long jdId) {
        QToDoList toDoList = QToDoList.toDoList;
        QJD jd = QJD.jD;

        ToDoList foundToDOList = queryFactory
                .selectFrom(toDoList)
                .join(toDoList.jd, jd).fetchJoin()
                .where(toDoList.id.eq(toDoListId)
                        .and(toDoList.jd.id.eq(jdId)))
                .fetchOne();

        return Optional.ofNullable(foundToDOList);
    }

    @Override
    public List<ToDoList> findToDoListsByJdIdAndCategoryWithJd(Long jdId, ToDoListType category) {
        QToDoList toDoList = QToDoList.toDoList;
        QJD jd = QJD.jD;

        return queryFactory
                .selectFrom(toDoList)
                .join(toDoList.jd, jd).fetchJoin()
                .where(toDoList.jd.id.eq(jdId)
                        .and(toDoList.category.eq(category)))
                .fetch();
    }

    @Override
    public List<ToDoList> findAllByIdsWithJd(List<Long> ids) {
        QToDoList toDoList = QToDoList.toDoList;
        QJD jd = QJD.jD;

        return queryFactory
                .selectFrom(toDoList)
                .join(toDoList.jd, jd).fetchJoin()
                .where(toDoList.id.in(ids))
                .fetch();
    }
}
