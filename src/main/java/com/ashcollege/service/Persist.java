package com.ashcollege.service;


import com.ashcollege.entities.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@Transactional
@Component
@SuppressWarnings("unchecked")
public class Persist {

    private static final Logger LOGGER = LoggerFactory.getLogger(Persist.class);

    private final SessionFactory sessionFactory;


    @Autowired
    public Persist(SessionFactory sf) {
        this.sessionFactory = sf;
    }

    public <T> void saveAll(List<T> objects) {
        for (T object : objects) {
            sessionFactory.getCurrentSession().saveOrUpdate(object);
        }
    }

    public <T> void remove(Object o){
        sessionFactory.getCurrentSession().remove(o);
    }

    public Session getQuerySession() {
        return sessionFactory.getCurrentSession();
    }

    public void save(Object object) {

        this.sessionFactory.getCurrentSession().saveOrUpdate(object);
        this.sessionFactory.getCurrentSession().flush();
    }

    public <T> T loadObject(Class<T> clazz, int oid) {
        return this.getQuerySession().get(clazz, oid);
    }

    public <T> List<T> loadList(Class<T> clazz)
    {
        return this.sessionFactory.getCurrentSession()
                .createQuery("FROM " + clazz.getSimpleName()).list();
    }
    public UserEntity getUserByUsernameAndPassword(String username, String password) {
        return this.sessionFactory.getCurrentSession()
                .createQuery("FROM UserEntity  " +
                        "WHERE username = :username " +
                        "AND password = :password", UserEntity.class)
                .setParameter("username", username)
                .setParameter("password", password)
                .uniqueResult();
    }
    public UserEntity getUserByUsername(String username) {
        return this.sessionFactory.getCurrentSession()
                .createQuery("FROM UserEntity " + " WHERE username = :username ", UserEntity.class)
                .setParameter("username", username)
                .uniqueResult();
    }
    public UserEntity getUserByToken(String token) {
        return this.sessionFactory.getCurrentSession()
                .createQuery("FROM UserEntity " +
                        "WHERE token = :token", UserEntity.class)
                .setParameter("token", token)
                .uniqueResult();
    }
    public UserEntity getUserById(int id) {
        return this.sessionFactory.getCurrentSession()
                .createQuery("FROM UserEntity " +
                        "WHERE id = :id", UserEntity.class)
                .setParameter("id", id)
                .uniqueResult();
    }
    public GameEntity getGameById(int id) {
        return this.sessionFactory.getCurrentSession()
                .createQuery("FROM GameEntity " +
                        "WHERE id = :id", GameEntity.class)
                .setParameter("id", id)
                .uniqueResult();
    }
    public GameEntity getGameByGameCode(String gameCode) {
        return this.sessionFactory.getCurrentSession()
                .createQuery("FROM GameEntity " +
                        "WHERE gameCode = :gameCode" + " AND deleted = false", GameEntity.class)
                .setParameter("gameCode", gameCode)
                .uniqueResult();
    }
    public List<UserEntity> getPlayersByGameId(int gameId) {
        return this.sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT gp.player FROM GamePlayerEntity gp " +
                                "WHERE gp.game.id = :id",
                        UserEntity.class)
                .setParameter("id", gameId)
                .getResultList();
    }







}