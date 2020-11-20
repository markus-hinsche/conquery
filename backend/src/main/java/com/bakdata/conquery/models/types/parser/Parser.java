package com.bakdata.conquery.models.types.parser;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
@Slf4j
public abstract class Parser<MAJOR_JAVA_TYPE> {
	private int lines = 0;
	private int nullLines = 0;
	
	public MAJOR_JAVA_TYPE parse(String v) throws ParsingException {
		if(v==null) {
			return null;
		}
		try {
			return parseValue(v);
		}
		catch(Exception e) {
			throw new ParsingException("Failed to parse '"+v+"' with "+this.getClass().getSimpleName(), e);
		}
	}
	
	protected abstract MAJOR_JAVA_TYPE parseValue(@Nonnull String value) throws ParsingException;

	protected void registerValue(MAJOR_JAVA_TYPE v) {};
	
	protected abstract CType<MAJOR_JAVA_TYPE> decideType();
	
	public CType<MAJOR_JAVA_TYPE> findBestType() {
		CType<MAJOR_JAVA_TYPE> dec = decideType();
		copyLineCounts(dec);
		return dec;
	}
	
	public MAJOR_JAVA_TYPE addLine(MAJOR_JAVA_TYPE v) {
		lines++;
		log.trace("Registering `{}` in line {}",v, lines);

		if(v == null) {
			nullLines++;
		}
		else {
			registerValue(v);
		}
		return v;
	}
	
	public void copyLineCounts(CType<?> type) {
		type.setLines(lines);
		type.setNullLines(nullLines);
	}
}
