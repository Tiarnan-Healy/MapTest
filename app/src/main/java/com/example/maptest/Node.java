package com.example.maptest;

public class Node {
    private int number;
    private float x;
    private float y;

    public Node(int number, float x, float y) {
        this.number = number;
        this.x = x;
        this.y = y;
    }

    public int getNumber() {
        return number;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
