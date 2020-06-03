package xyz.phanta.tconevo.init;

import io.github.phantamanta44.libnine.InitMe;
import io.github.phantamanta44.libnine.util.math.MathUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xyz.phanta.tconevo.TconEvoConfig;
import xyz.phanta.tconevo.TconEvoMod;
import xyz.phanta.tconevo.constant.NameConst;
import xyz.phanta.tconevo.potion.PotionDispellable;
import xyz.phanta.tconevo.potion.PotionUndispellable;

public class TconEvoPotions {

    private static final String PREFIX = "effect.tconevo.";

    public static final Potion IMMORTALITY = new PotionUndispellable(false, 0xebc083)
            .setBeneficial().setPotionName(PREFIX + NameConst.POTION_IMMORTALITY);
    public static final Potion MORTAL_WOUNDS = new PotionDispellable(true, 0x5f5d8e)
            .setPotionName(PREFIX + NameConst.POTION_MORTAL_WOUNDS);
    public static final Potion DAMAGE_REDUCTION = new PotionDispellable(false, 0x5a059a)
            .setBeneficial().setPotionName(PREFIX + NameConst.POTION_DAMAGE_REDUCTION);

    @InitMe
    public static void init() {
        MinecraftForge.EVENT_BUS.register(new TconEvoPotions());
    }

    @SubscribeEvent
    public void onRegisterPotions(RegistryEvent.Register<Potion> event) {
        event.getRegistry().registerAll(
                IMMORTALITY.setRegistryName(TconEvoMod.MOD_ID, NameConst.POTION_IMMORTALITY),
                MORTAL_WOUNDS.setRegistryName(TconEvoMod.MOD_ID, NameConst.POTION_MORTAL_WOUNDS),
                DAMAGE_REDUCTION.setRegistryName(TconEvoMod.MOD_ID, NameConst.POTION_DAMAGE_REDUCTION));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityHurt(LivingDamageEvent event) {
        DamageSource dmgSrc = event.getSource();
        // don't block void damage or /kill
        if (dmgSrc == DamageSource.OUT_OF_WORLD
                || (event.getAmount() == Float.MAX_VALUE && dmgSrc.isUnblockable() && dmgSrc.canHarmInCreative())) {
            return;
        }
        EntityLivingBase entity = event.getEntityLiving();
        float amount = event.getAmount();
        PotionEffect dmgReduction = entity.getActivePotionEffect(DAMAGE_REDUCTION);
        if (dmgReduction != null) {
            amount /= 1F + (dmgReduction.getAmplifier() + 1F)
                    * (float)TconEvoConfig.general.effectDamageReductionDividerIncrement;
        }
        if (entity.getActivePotionEffect(IMMORTALITY) != null) {
            // never let damage drop the player below 1 health
            amount = MathUtils.clamp(entity.getHealth() - 1F, 0F, amount);
        }
        event.setAmount(amount);
    }

    // just in case something gets around onEntityHurt
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityDeath(LivingDeathEvent event) {
        if (event.getSource() == DamageSource.OUT_OF_WORLD) {
            return;
        }
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.getActivePotionEffect(IMMORTALITY) != null) {
            entity.setHealth(1F);
            entity.hurtResistantTime = entity.maxHurtResistantTime;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityHeal(LivingHealEvent event) {
        if (event.getEntityLiving().getActivePotionEffect(MORTAL_WOUNDS) != null
                && TconEvoConfig.general.effectMortalWoundsHealReduction > 0D) {
            float amount = event.getAmount() * (1F - (float)TconEvoConfig.general.effectMortalWoundsHealReduction);
            if (amount <= 0F) {
                event.setAmount(0F);
                event.setCanceled(true);
            } else {
                event.setAmount(amount);
            }
        }
    }

}
