package com.example.siggtrat.configuracao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;


public class MetadadosPagina {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private String sort;
    public MetadadosPagina() {}

    public static MetadadosPagina from(Page<?> p) {
        MetadadosPagina m = new MetadadosPagina();
        m.page = p.getNumber();
        m.size = p.getSize();
        m.totalElements = p.getTotalElements();
        m.totalPages = p.getTotalPages();
        m.first = p.isFirst();
        m.last = p.isLast();
        m.sort = ordenacaoComoTexto(p.getSort());
        return m;
    }

    private static String ordenacaoComoTexto(Sort sort) {
        if (sort == null || sort.isUnsorted()) return null;
        return sort.toString();
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
