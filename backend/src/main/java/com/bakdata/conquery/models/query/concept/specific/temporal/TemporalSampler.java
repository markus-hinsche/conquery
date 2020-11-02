package com.bakdata.conquery.models.query.concept.specific.temporal;

import java.time.LocalDate;
import java.util.OptionalInt;
import java.util.Random;

import com.bakdata.conquery.models.common.BitMapCDateSet;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;

/**
 * A class implementing several sampling schemes for {@link BitMapCDateSet}.
 */
public enum TemporalSampler {
	/**
	 * Sampler that returns the earliest date of the {@link BitMapCDateSet}, if present, or empty if the Set has no lowerbound.
	 */
	EARLIEST {
		/**
		 * Retrieves the earliest date contained in {@link BitMapCDateSet}.
		 * @param data the set to be sampled from.
		 * @return the earliest date contained in {@link BitMapCDateSet}.
		 */
		@Override
		public OptionalInt sample(BitMapCDateSet data) {
			if (data.isEmpty()) {
				return OptionalInt.empty();
			}

			final CDateRange span = data.span();

			if (!span.hasLowerBound()) {
				return OptionalInt.empty();
			}

			return OptionalInt.of(span.getMinValue());
		}
	},
	/**
	 * Sampler that returns the latest date of the {@link BitMapCDateSet}, if present, or empty if the Set has no upperbound.
	 */
	LATEST {
		/**
		 * Retrieves the latest date contained in {@link BitMapCDateSet}.
		 * @param data the set to be sampled from.
		 * @return the latest date contained in {@link BitMapCDateSet}.
		 */
		@Override
		public OptionalInt sample(BitMapCDateSet data) {
			if (data.isEmpty()) {
				return OptionalInt.empty();
			}

			final CDateRange span = data.span();

			if (!span.hasUpperBound()) {
				return OptionalInt.empty();
			}

			return OptionalInt.of(span.getMaxValue());
		}
	},
	/**
	 * Sampler that returns a random date that is inside {@link BitMapCDateSet}.
	 */
	RANDOM {
		/**
		 * A source for random values.
		 */
		private Random random = new Random();

		/**
		 *
		 * @param data the set to be sampled from.
		 * @return a random date contained in {@code data}.
		 */
		@Override
		public OptionalInt sample(BitMapCDateSet data) {
			if (data.isEmpty()) {
				return OptionalInt.empty();
			}

			CDateRange span = data.span();

			if(span.isAll())
				return OptionalInt.empty();

			int lower;

			if (span.hasLowerBound()) {
				lower = span.getMinValue();
			}
			else {
				lower = CDate.ofLocalDate(LocalDate.MIN);
			}


			int upper;

			if (span.hasUpperBound()) {
				upper = span.getMaxValue();
			}
			else {
				upper = CDate.ofLocalDate(LocalDate.MAX);
			}


			if (upper == lower) {
				return OptionalInt.of(upper);
			}

			int sample;

			// Sample new values as long as they are not inside the data.
			do {
				sample = lower + random.nextInt(upper - lower);
			} while (!data.contains(sample));


			return OptionalInt.of(sample);
		}
	};

	/**
	 * Get a date from within the {@link BitMapCDateSet} that is produced according to a sampling scheme.
	 * @param data the set to be sampled from.
	 * @return the date fitting the sampling criteria. Or {@link OptionalInt#empty()} if none is found.
	 */
	public abstract OptionalInt sample(BitMapCDateSet data);

}
