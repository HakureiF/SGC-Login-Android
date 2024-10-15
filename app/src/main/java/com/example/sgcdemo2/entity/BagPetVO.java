package com.example.sgcdemo2.entity;

import java.util.List;

public class BagPetVO {
    private Integer id;
    private Integer catchTime;
    private Integer level;
    private Integer effectID;
    private List<Integer> marks;
    private List<Skill> skillArray;
    private Skill hideSkill;
    private Integer state; //0——无 1——被ban 2——首发 3——出战
    private List<MarkVO> bindMarks;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCatchTime() {
        return catchTime;
    }

    public void setCatchTime(Integer catchTime) {
        this.catchTime = catchTime;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getEffectID() {
        return effectID;
    }

    public void setEffectID(Integer effectID) {
        this.effectID = effectID;
    }

    public List<Integer> getMarks() {
        return marks;
    }

    public void setMarks(List<Integer> marks) {
        this.marks = marks;
    }

    public List<Skill> getSkillArray() {
        return skillArray;
    }

    public void setSkillArray(List<Skill> skillArray) {
        this.skillArray = skillArray;
    }

    public Skill getHideSkill() {
        return hideSkill;
    }

    public void setHideSkill(Skill hideSkill) {
        this.hideSkill = hideSkill;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public List<MarkVO> getBindMarks() {
        return bindMarks;
    }

    public void setBindMarks(List<MarkVO> bindMarks) {
        this.bindMarks = bindMarks;
    }
}
