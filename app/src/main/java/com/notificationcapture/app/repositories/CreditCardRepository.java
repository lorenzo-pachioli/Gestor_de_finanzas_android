package com.notificationcapture.app.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.notificationcapture.app.interfaces.GsonAccess;
import com.notificationcapture.app.models.CreditCard;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CreditCardRepository implements GsonAccess {

    private SharedPreferences prefs;
    private Gson gson;

    public CreditCardRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<CreditCard> getCreditCards() {
        String json = prefs.getString(KEY_CREDIT_CARDS, null);
        if (json == null)
            return new ArrayList<>();
        Type type = new TypeToken<List<CreditCard>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void addCreditCard(com.notificationcapture.app.models.CreditCard card) {
        List<com.notificationcapture.app.models.CreditCard> cards = getCreditCards();
        cards.add(card);
        saveCreditCards(cards);
    }

    public void deleteCreditCard(String id) {
        List<com.notificationcapture.app.models.CreditCard> cards = getCreditCards();
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getId().equals(id)) {
                cards.remove(i);
                break;
            }
        }
        saveCreditCards(cards);
    }

    public void updateCreditCard(com.notificationcapture.app.models.CreditCard updatedCard) {
        List<com.notificationcapture.app.models.CreditCard> cards = getCreditCards();
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getId().equals(updatedCard.getId())) {
                cards.set(i, updatedCard);
                break;
            }
        }
        saveCreditCards(cards);
    }

    private void saveCreditCards(List<com.notificationcapture.app.models.CreditCard> cards) {
        prefs.edit().putString(KEY_CREDIT_CARDS, gson.toJson(cards)).apply();
    }
}
