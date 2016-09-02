//package uk.ac.ebi.spot.security;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//
///**
// * @author Simon Jupp
// * @date 10/08/2016
// * Samples, Phenotypes and Ontologies Team, EMBL-EBI
// */
//@EnableWebSecurity
//public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
//
//
//    @Override
//   	protected void configure(HttpSecurity http) throws Exception {
//   		http
//   				.authorizeRequests()
//   					.antMatchers("/css/**", "/index").permitAll()
//   					.antMatchers("/user/**").hasRole("USER")
//   					.and()
//   				.formLogin().loginPage("/login").failureUrl("/login-error");
//   	}
//
//	@Autowired
//	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//		auth
//			.inMemoryAuthentication()
//				.withUser("user").password("password").roles("USER");
//	}
//}
