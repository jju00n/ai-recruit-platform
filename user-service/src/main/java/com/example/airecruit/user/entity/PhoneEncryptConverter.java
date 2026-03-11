package com.example.airecruit.user.entity;

import com.example.airecruit.user.util.CryptoUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter(autoApply = false)
public class PhoneEncryptConverter implements AttributeConverter<String, String> {
    private static CryptoUtils crypto;

    @Autowired
    public PhoneEncryptConverter(CryptoUtils cryptoUtils) {
        crypto = cryptoUtils;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return (crypto == null || attribute == null) ? attribute : crypto.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return (crypto == null || dbData == null) ? dbData : crypto.decrypt(dbData);
    }
}
