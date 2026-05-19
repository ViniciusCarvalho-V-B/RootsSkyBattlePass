package com.rootssky.battlepass.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int quantidade) {
        this.itemStack = new ItemStack(material, quantidade);
        this.meta = itemStack.getItemMeta();
    }

    public ItemBuilder nome(Component nome) {
        meta.displayName(nome);
        return this;
    }

    public ItemBuilder lore(List<Component> lore) {
        meta.lore(lore);
        return this;
    }

    public ItemBuilder addLore(Component linha) {
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(linha);
        meta.lore(lore);
        return this;
    }

    public ItemBuilder encantar(Enchantment encantamento, int nivel) {
        meta.addEnchant(encantamento, nivel, true);
        return this;
    }

    public ItemBuilder brilho() {
        meta.addEnchant(Enchantment.SHARPNESS, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder addGlow() {
        meta.addEnchant(Enchantment.SHARPNESS, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder addGoldGlow() {
        meta.addEnchant(Enchantment.SHARPNESS, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder esconderAtributos() {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        return this;
    }

    public ItemBuilder customModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    public ItemStack build() {
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
