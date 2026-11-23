
package com.enset.oraclecommerce.config;

import com.enset.oraclecommerce.oracle.OracleEventBuffer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OracleEventBufferConfig {

    @Bean
    public OracleEventBuffer oracleEventBuffer() {
        return new OracleEventBuffer();
    }
}
