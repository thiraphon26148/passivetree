package com.example.passivetree.data;

import com.example.passivetree.tree.SkillNode;
import com.example.passivetree.tree.SkillTree;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class PlayerSkills {
    private String classId = "";
    private final Set<String> allocated = new LinkedHashSet<>();
    private int highestLevel;
    private int bonusPoints;

    public String getClassId() { return classId; }
    public void setClassId(String id) { this.classId = id; }
    public Set<String> getAllocated() { return allocated; }
    public boolean isAllocated(String id) { return allocated.contains(id); }
    public int getHighestLevel() { return highestLevel; }
    public void setHighestLevel(int v) { this.highestLevel = v; }
    public int getBonusPoints() { return bonusPoints; }
    public void addBonusPoints(int v) { this.bonusPoints += v; }

    public int totalPoints() {
        return highestLevel + bonusPoints;
    }

    public int spentPoints(SkillTree tree) {
        SkillTree.ClassDef cd = tree.classById(classId);
        int spent = 0;
        for (String id : allocated) {
            if (cd != null && id.equals(cd.start())) continue; // the class start node is free
            SkillNode n = tree.nodes.get(id);
            if (n != null) spent += n.cost();
        }
        return spent;
    }

    public int availablePoints(SkillTree tree) {
        return totalPoints() - spentPoints(tree);
    }

    public void setAll(String classId, Collection<String> allocated, int highestLevel, int bonusPoints) {
        Collection<String> copy = new ArrayList<>(allocated); // safe even if same collection
        this.classId = classId;
        this.allocated.clear();
        this.allocated.addAll(copy);
        this.highestLevel = highestLevel;
        this.bonusPoints = bonusPoints;
    }

    public void copyFrom(PlayerSkills o) {
        setAll(o.classId, o.allocated, o.highestLevel, o.bonusPoints);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("class", classId);
        ListTag list = new ListTag();
        for (String id : allocated) list.add(StringTag.valueOf(id));
        tag.put("allocated", list);
        tag.putInt("highestLevel", highestLevel);
        tag.putInt("bonusPoints", bonusPoints);
        return tag;
    }

    public void load(CompoundTag tag) {
        classId = tag.getString("class");
        allocated.clear();
        ListTag list = tag.getList("allocated", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) allocated.add(list.getString(i));
        highestLevel = tag.getInt("highestLevel");
        bonusPoints = tag.getInt("bonusPoints");
    }
}
