package com.example.passivetree.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerSkillsProvider implements ICapabilitySerializable<CompoundTag> {
    private final PlayerSkills skills = new PlayerSkills();
    private final LazyOptional<PlayerSkills> optional = LazyOptional.of(() -> skills);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.PLAYER_SKILLS) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return skills.save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        skills.load(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
