package br.com.libraryapi.api.controller;

import static br.com.libraryapi.service.LoanServiceTest.createLoan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.libraryapi.api.dto.LoanDTO;
import br.com.libraryapi.api.dto.LoanFilterDTO;
import br.com.libraryapi.api.dto.ReturnedLoanDTO;
import br.com.libraryapi.exception.BusinessException;
import br.com.libraryapi.model.entity.Book;
import br.com.libraryapi.model.entity.Loan;
import br.com.libraryapi.service.BookService;
import br.com.libraryapi.service.LoanService;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@WebMvcTest(controllers = LoanController.class)
@AutoConfigureMockMvc
public class LoanControllerTest {

	private static String LOAN_API = "/api/loans";
	
	@Autowired
	private MockMvc mvc;
	
	@MockBean
	private BookService bookservice;

	@MockBean
	private LoanService loanService;
	
	@Test
	@DisplayName("Deve realizar um emprestimo")
	public void createLoanTest() throws Exception{
	
		// cenario
		LoanDTO dto = LoanDTO.builder().isbn("123").email("custumer@email.com").customer("Fulano").build();
		String json = new ObjectMapper().writeValueAsString(dto);
	
		Book book = Book.builder().id(1l).isbn("123").build();
		BDDMockito.given(bookservice.getBookByIsbn("123"))
				  .willReturn(Optional.of(book));
		
		Loan loan = Loan.builder().id(1l).customer("Fulano").book(book).loanDate(LocalDate.now()).build();
		BDDMockito.given(loanService.save(Mockito.any(Loan.class))).willReturn(loan);
		
		// execucao
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.post(LOAN_API)
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json);
		
		// verificacao
		mvc.perform(request)
			.andExpect(status().isCreated())
			.andExpect(content().string("1"));
	}
	
	@Test
	@DisplayName("Deve retornar erro ao rtentar fazer emprestimo de um livro inexistente")
	public void invalidIsbnCreateLoanTest() throws Exception {
		
		// cenario
		LoanDTO dto = LoanDTO.builder().isbn("123").customer("Fulano").build();
		String json = new ObjectMapper().writeValueAsString(dto);
		BDDMockito.given(bookservice.getBookByIsbn("123"))
		          .willReturn(Optional.empty());

		// execucao
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(LOAN_API)
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json);
				
		// verificacao
		mvc.perform(request)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("errors", Matchers.hasSize(1)))
			.andExpect(jsonPath("errors[0]").value("Book not found for passed ISBN"));
		
	}
	
	@Test
	@DisplayName("Deve retornar erro ao rtentar fazer emprestimo de um livro emprestado")
	public void loanedBookErrorOnCreateLoanTest() throws Exception {
		
		// cenario
		LoanDTO dto = LoanDTO.builder().isbn("123").customer("Fulano").build();
		String json = new ObjectMapper().writeValueAsString(dto);
		
		Book book = Book.builder().id(1l).isbn("123").build();
		BDDMockito.given(bookservice.getBookByIsbn("123"))
				  .willReturn(Optional.of(book));
		
		BDDMockito.given(loanService.save(Mockito.any(Loan.class)))
				  .willThrow(new BusinessException("Book already loaned"));

		// execucao
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(LOAN_API)
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json);
				
		// verificacao
		mvc.perform(request)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("errors", Matchers.hasSize(1)))
			.andExpect(jsonPath("errors[0]").value("Book already loaned"));
		
	}
	
	@Test
	@DisplayName("Deve retornar um livro")
	public void returnBookTest() throws Exception {
		
		// cenario 
		ReturnedLoanDTO dto = ReturnedLoanDTO.builder().returned(true).build();
		Loan loan = Loan.builder().id(1l).build();
		BDDMockito.given(loanService.getById(Mockito.anyLong()))
				  .willReturn(Optional.of(loan));
		
		String json = new ObjectMapper().writeValueAsString(dto);
		
		// execucao
		mvc.perform(
				patch(LOAN_API.concat("/1"))
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON)
					.content(json)
			).andExpect(status().isOk());
		
		// verificacao
		Mockito.verify(loanService, Mockito.times(1)).update(loan);
	}
	
	@Test
	@DisplayName("Deve retornar 404 quando tentar devolver um livro inexistente")
	public void returnInexistentBookTest() throws Exception {
		
		// cenario 
		ReturnedLoanDTO dto = ReturnedLoanDTO.builder().returned(true).build();
		
		BDDMockito.given(loanService.getById(Mockito.anyLong()))
				  .willReturn(Optional.empty());
		
		String json = new ObjectMapper().writeValueAsString(dto);
		
		// execucao
		mvc.perform(
				patch(LOAN_API.concat("/1"))
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON)
					.content(json)
			)
		// verificacao
		.andExpect(status().isNotFound());
		
	}
	
	@Test
	@DisplayName("deve filtrar emprestimos")
	public void findLoanBooksTest() throws Exception {

		// cenario
		Long id = 1l;
		Loan loan = createLoan();
		loan.setId(id);
		Book book = Book.builder().id(1l).isbn("321").build();
		loan.setBook(book);

		BDDMockito.given( loanService.find(Mockito.any(LoanFilterDTO.class), Mockito.any(Pageable.class)) )
				  .willReturn( new PageImpl<Loan>( Arrays.asList(loan), PageRequest.of(0, 10), 1) );
		
		String queryString = String.format("?isbn=%s&customer=%s&page=0&size=10",
				book.getIsbn(), loan.getCustomer());
		
		// execucao
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(LOAN_API.concat(queryString))
				.accept(MediaType.APPLICATION_JSON);
		
		// verificacao
		mvc.perform(request).andExpect(status().isOk())
				.andExpect(jsonPath("content", hasSize(1)))
				.andExpect(jsonPath("totalElements").value(1))
				.andExpect(jsonPath("pageable.pageSize").value(10))
				.andExpect(jsonPath("pageable.pageNumber").value(0));

	}
	
}


