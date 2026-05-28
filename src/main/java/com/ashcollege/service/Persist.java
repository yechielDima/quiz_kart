package com.ashcollege.service;

import com.ashcollege.entities.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Component
@SuppressWarnings("unchecked")
public class Persist {

    @Autowired
    private SessionFactory sessionFactory;

    private Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    public void save(Object object) {
        getSession().saveOrUpdate(object);
    }

    public void flush() {
        getSession().flush();
    }

    public UserEntity getUserByToken(String token) {
        return (UserEntity) getSession()
                .createQuery("FROM UserEntity WHERE token = :token AND deleted = false")
                .setParameter("token", token)
                .uniqueResult();
    }

    public UserEntity getUserByUsernameAndPassword(String username, String password) {
        return (UserEntity) getSession()
                .createQuery("FROM UserEntity WHERE username = :username AND password = :password AND deleted = false")
                .setParameter("username", username)
                .setParameter("password", password)
                .uniqueResult();
    }

    public UserEntity getUserByUsername(String username) {
        return (UserEntity) getSession()
                .createQuery("FROM UserEntity WHERE username = :username AND deleted = false")
                .setParameter("username", username)
                .uniqueResult();
    }

    public GameEntity getGameById(int id) {
        return (GameEntity) getSession()
                .createQuery("FROM GameEntity WHERE id = :id AND deleted = false")
                .setParameter("id", id)
                .uniqueResult();
    }

    public GameEntity getGameByGameCode(String gameCode, int status) {
        return (GameEntity) getSession()
                .createQuery("FROM GameEntity WHERE gameCode = :gameCode AND status = :status AND deleted = false")
                .setParameter("gameCode", gameCode)
                .setParameter("status", status)
                .uniqueResult();
    }

    public boolean doesGameCodeExist(String gameCode) {
        return getSession()
                .createQuery("FROM GameEntity WHERE gameCode = :gameCode AND deleted = false")
                .setParameter("gameCode", gameCode)
                .uniqueResult() != null;
    }

    public List<GamePlayerEntity> getGamePlayersByGameId(int gameId) {
        return getSession()
                .createQuery("FROM GamePlayerEntity WHERE game.id = :gameId AND deleted = false")
                .setParameter("gameId", gameId)
                .list();
    }

    public GamePlayerEntity getGamePlayerByGameAndUser(int gameId, int userId) {
        return (GamePlayerEntity) getSession()
                .createQuery("FROM GamePlayerEntity WHERE game.id = :gameId AND player.id = :userId AND deleted = false")
                .setParameter("gameId", gameId)
                .setParameter("userId", userId)
                .uniqueResult();
    }

    public List<GamePlayerEntity> getGamePlayersByUserId(int userId) {
        return getSession()
                .createQuery("FROM GamePlayerEntity WHERE player.id = :userId AND deleted = false ORDER BY id DESC")
                .setParameter("userId", userId)
                .list();
    }

    public List<PlayerAnswerEntity> getAnswersByGamePlayerId(int gamePlayerId) {
        return getSession()
                .createQuery("FROM PlayerAnswerEntity WHERE gamePlayer.id = :gpId AND deleted = false")
                .setParameter("gpId", gamePlayerId)
                .list();
    }

    public List<PlayerAnswerEntity> getAllAnswersByUserId(int userId) {
        return getSession()
                .createQuery("FROM PlayerAnswerEntity pa WHERE pa.gamePlayer.player.id = :userId AND pa.deleted = false")
                .setParameter("userId", userId)
                .list();
    }

    public List<QuestionTemplateEntity> getAllTemplates() {
        return getSession()
                .createQuery("FROM QuestionTemplateEntity WHERE deleted = false")
                .list();
    }

    public List<QuestionWordEntity> getAllWords() {
        return getSession()
                .createQuery("FROM QuestionWordEntity WHERE deleted = false")
                .list();
    }
}