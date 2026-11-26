package com.turnosodontologia.domain.identidad;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/**
 * Persiste {@link Ambito} en minúsculas ({@code propio}, {@code todos}, {@code config}), como exige el
 * CHECK {@code ck_permisos_ambito} de V001. Con {@code @Enumerated(STRING)} Hibernate comparaba contra
 * {@code PROPIO} y ninguna consulta por ámbito encontraba los permisos sembrados en PostgreSQL.
 */
@Converter
public class AmbitoConverter implements AttributeConverter<Ambito, String> {

    @Override
    public String convertToDatabaseColumn(Ambito ambito) {
        return ambito == null ? null : ambito.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Ambito convertToEntityAttribute(String valor) {
        return valor == null ? null : Ambito.valueOf(valor.trim().toUpperCase(Locale.ROOT));
    }
}
