package com.appspot.c_three_games.domain.war;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class Player {
  @Id
  private Long id;

  private int num;

  private String name;

  private String regId;

  @Parent
  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  private Key<Game> gameId;

  private List<Card> handDeck;

  private List<Card> discardDeck;

  private State state;

  public enum State {
    JOINING, PLAYING, PLAYED, WAR1, WAR2, WAR3, NOTINWAR, LOST, WON;
  }

  @Index
  private Date created;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public int getNum() {
    return num;
  }

  public void setNum(int num) {
    this.num = num;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Key<Game> getGameId() {
    return gameId;
  }

  public void setGameId(Key<Game> gameId) {
    this.gameId = gameId;
  }

  public List<Card> getHandDeck() {
    return handDeck;
  }

  public void setHandDeck(List<Card> handDeck) {
    this.handDeck = handDeck;
  }

  public List<Card> getDiscardDeck() {
    return discardDeck;
  }

  public void setDiscardDeck(List<Card> discardDeck) {
    this.discardDeck = discardDeck;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public String getRegId() {
    return regId;
  }

  public void setRegId(String regId) {
    this.regId = regId;
  }

  public Player() {
    handDeck = new ArrayList<Card>();
    discardDeck = new ArrayList<Card>();
  }
}
