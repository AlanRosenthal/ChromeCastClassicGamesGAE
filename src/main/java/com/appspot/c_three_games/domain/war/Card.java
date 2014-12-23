package com.appspot.c_three_games.domain.war;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Card {

    private String name;
    private String suit;
    private String rank;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSuit() {
        return suit;
    }

    public void setSuit(String suit) {
        this.suit = suit;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public int getRankInt() {
        if (rank.equals("A"))
            return 14;
        else if (rank.equals("2"))
            return 2;
        else if (rank.equals("3"))
            return 3;
        else if (rank.equals("4"))
            return 4;
        else if (rank.equals("5"))
            return 5;
        else if (rank.equals("6"))
            return 6;
        else if (rank.equals("7"))
            return 7;
        else if (rank.equals("8"))
            return 8;
        else if (rank.equals("9"))
            return 9;
        else if (rank.equals("10"))
            return 10;
        else if (rank.equals("J"))
            return 11;
        else if (rank.equals("Q"))
            return 12;
        else if (rank.equals("K"))
            return 13;
        else
            return -1;
    }

    public Card(String suit, String rank) {
        this.suit = suit;
        this.rank = rank;
        this.name = rank + suit;
    }

    public Card() {
    }

    public static List<Card> fullDeck() {
        List<Card> deck = new ArrayList<Card>();
        String[] suit = { "C", "D", "H", "S" };
        String[] rank = { "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K" };
        for (int i = 0; i < 13; i++) {
            for (int j = 0; j < 4; j++) {
                deck.add(new Card(suit[j], rank[i]));
            }
        }
        return deck;
    }

    public static List<Card> shuffleDeck(List<Card> cards) {
        Random rand = new Random();
        for (int i = 0; i < cards.size(); i++) {
            int j = rand.nextInt(i + 1);
            Card tempA = cards.get(i);
            Card tempB = cards.get(j);
            cards.set(j, tempA);
            cards.set(i, tempB);
        }
        return cards;
    }

}