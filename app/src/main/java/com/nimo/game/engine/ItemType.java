package com.nimo.game.engine;

import com.nimo.game.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Tipos de produtos que caem do céu.
 * Para adicionar um novo produto basta incluir a imagem em res/drawable-nodpi,
 * o nome em strings.xml e uma nova constante aqui.
 */
public enum ItemType {
    //          imagem                    nome                    categoria              pontos  peso de sorteio
    BOTTLE(R.drawable.item_bottle, R.string.item_bottle, ItemCategory.CORRECT, 5, 3),
    CAN(R.drawable.item_can, R.string.item_can, ItemCategory.CORRECT, 5, 3),
    PHONE(R.drawable.item_phone, R.string.item_phone, ItemCategory.CORRECT, 10, 1),
    STEAK(R.drawable.item_steak, R.string.item_steak, ItemCategory.WRONG, 0, 1),
    BANANA_PEEL(R.drawable.item_banana, R.string.item_banana, ItemCategory.WRONG, 0, 1);

    public final int drawableRes;
    public final int nameRes;
    public final ItemCategory category;
    public final int points;
    public final int spawnWeight;

    ItemType(int drawableRes, int nameRes, ItemCategory category, int points, int spawnWeight) {
        this.drawableRes = drawableRes;
        this.nameRes = nameRes;
        this.category = category;
        this.points = points;
        this.spawnWeight = spawnWeight;
    }

    public boolean isCorrect() {
        return category == ItemCategory.CORRECT;
    }

    public static List<ItemType> ofCategory(ItemCategory category) {
        List<ItemType> list = new ArrayList<>();
        for (ItemType type : values()) {
            if (type.category == category) {
                list.add(type);
            }
        }
        return list;
    }
}
