package icu.azim.wagrapple.item.enchantments;

import icu.azim.wagrapple.item.GrappleItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class BoostPowerEnchantment extends Enchantment{

	public BoostPowerEnchantment(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes) {
		super(weight, type, slotTypes);
	}
	
	@Override
	public boolean isAcceptableItem(ItemStack stack) {
		return stack.getItem() instanceof GrappleItem;
	}
	
	@Override
	public int getMinPower(int level) {
		return level*11;
	}
	
	@Override
	public int getMaxPower(int level) {
		return getMinPower(level)+20;
	}
	
	@Override
	public int getMaxLevel() {
		return 3;
	}
}
