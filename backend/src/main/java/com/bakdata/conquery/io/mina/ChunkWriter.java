package com.bakdata.conquery.io.mina;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.google.common.primitives.Ints;

import io.dropwizard.util.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class ChunkWriter extends ProtocolEncoderAdapter {

	public static final int HEADER_SIZE = Integer.BYTES + Byte.BYTES + 2*Long.BYTES;
	public static final byte LAST_MESSAGE = 1;
	public static final byte CONTINUED_MESSAGE = 0;
	
	@Getter @Setter
	private IoBuffer buffer = IoBuffer.allocate(Ints.checkedCast(Size.megabytes(32).toBytes()), false).position(HEADER_SIZE);
	@SuppressWarnings("rawtypes")
	private final CQCoder coder;

	@SuppressWarnings("unchecked")
	@Override
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		synchronized (buffer) {
			Chunkable ch = coder.encode(message);
			try(ChunkOutputStream cos = new ChunkOutputStream(ch.getId(), out)) {
				ch.writeMessage(cos);
			}
		}
	}

	@RequiredArgsConstructor
	private class ChunkOutputStream extends OutputStream {
		private final UUID id;
		private final ProtocolEncoderOutput out;
		
		private boolean closed = false;
		
		private void newBuffer(int required) {
			if(buffer.remaining()<required) {
				finishBuffer(false);
			}
		}

		private void finishBuffer(boolean end) {
			buffer.flip();
			if(buffer.remaining() - HEADER_SIZE == 0) {
				throw new IllegalStateException();
			}
			buffer.put(0, end ? LAST_MESSAGE : CONTINUED_MESSAGE);
			buffer.putInt(Byte.BYTES, buffer.remaining() - HEADER_SIZE);
			buffer.putLong(Byte.BYTES+Integer.BYTES, id.getMostSignificantBits());
			buffer.putLong(Byte.BYTES+Integer.BYTES+Long.BYTES, id.getLeastSignificantBits());
			out.write(buffer);
			out.flush().awaitUninterruptibly();
			buffer.clear();
			buffer.position(HEADER_SIZE);
		}

		@Override
		public void write(int b) throws IOException {
			if(closed) {
				throw new IllegalStateException();
			}
			newBuffer(1);
			buffer.put((byte)b);
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if(closed) {
				throw new IllegalStateException();
			}
			if (b == null) {
				throw new NullPointerException();
			} else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return;
			}
			
			while(len > 0) {
				if(!buffer.hasRemaining()) {
					newBuffer(len);
				}
				
				int write = Math.min(len, buffer.remaining());
				buffer.put(b, off, write);
				len -= write;
				off += write;
			}
		}
		
		@Override
		public void close() throws IOException {
			if(!closed) {
				finishBuffer(true);
				closed = true;
			}
		}
	}
}