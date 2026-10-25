package org.kirpich.smart_profile.service;

/** Пункт запроса дополнительной информации. type: TEXT — ответ текстом, FILE — загрузить документ с doc_type = key. */
public record InfoItem(String key, String label, String type) {

    public static final String TEXT = "TEXT";
    public static final String FILE = "FILE";

    public boolean isFile() {
        return FILE.equals(type);
    }
}
