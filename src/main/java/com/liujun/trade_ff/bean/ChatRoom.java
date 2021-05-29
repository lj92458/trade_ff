package com.liujun.trade_ff.bean;

import java.util.*;

public class ChatRoom<E> implements Queue<E> {
    private int limit;//保存的消息数量的最大值
    private Queue<E> queue;//消息集合
    private Set<String> userSet;//房间中有哪些人
    /**是否应该删除房间。
     * onbeforeunload事件，调用后台url销毁房间时，后台不立即销毁房间，而是要延迟。 needRemove=true;
       onload事件调用一个url，负责让needRemove=false ，目的是取消这个被延迟的任务。
     */
    private boolean needRemove=true;

    public ChatRoom(int limit) {
        this.limit = limit;
        this.queue = new LinkedList<E>();
        this.userSet = new HashSet<String>();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return queue.iterator();
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return queue.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return queue.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(0);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return queue.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return queue.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public boolean offer(E e) {
        if (queue.size() >= limit) {
            queue.poll();
        }
        return queue.offer(e);
    }

    @Override
    public E remove() {
        return queue.remove();
    }

    @Override
    public E poll() {
        return queue.poll();
    }

    @Override
    public E element() {
        return queue.element();
    }

    @Override
    public E peek() {
        return queue.peek();
    }

    public int getLimit() {
        return this.limit;
    }

    public int getUserCount() {
        return userSet.size();
    }

    public Set<String> getUserSet() {
        return userSet;
    }

    public void setUserSet(Set<String> userSet) {
        this.userSet = userSet;
    }

    public void addUser(String accountNo){
        if(accountNo != null){
            userSet.add(accountNo);
        }
    }

    public void removeUser(String accountNo){

        userSet.remove(accountNo);
    }

    public void removeAllUser(){
        userSet.clear();
    }

    public boolean isNeedRemove() {
        return needRemove;
    }

    public void setNeedRemove(boolean needRemove) {
        this.needRemove = needRemove;
    }
}