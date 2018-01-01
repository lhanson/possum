package io.github.lhanson.possum.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RandomConfig {
	Logger log = LoggerFactory.getLogger(this.class)
	@Value('${random-seed}')
	Long seed

	/**
	 * Use a single pseudo-random number generator across the app. This allows
	 * for determinism and repeatability with a known seed.
	 * @return an instance of {@link Random}
	 */
	@Bean
	Random random() {
		if (seed) {
			log.debug "Initializing RNG with provided seed: $seed"
		} else {
			seed = System.currentTimeMillis()
			log.debug "Initializing RNG with current time $seed"
		}
		new Random(seed)
	}
}
