package com.example.sgcdemo2.entity;

public class MarkVO {
    private Integer _markID;
    private Integer _bindMoveID; //宝石绑定的技能id，如果没有宝石则为0
    private Integer _obtainTime;

    public Integer get_markID() {
        return _markID;
    }

    public void set_markID(Integer _markID) {
        this._markID = _markID;
    }

    public Integer get_bindMoveID() {
        return _bindMoveID;
    }

    public void set_bindMoveID(Integer _bindMoveID) {
        this._bindMoveID = _bindMoveID;
    }

    public Integer get_obtainTime() {
        return _obtainTime;
    }

    public void set_obtainTime(Integer _obtainTime) {
        this._obtainTime = _obtainTime;
    }
}
