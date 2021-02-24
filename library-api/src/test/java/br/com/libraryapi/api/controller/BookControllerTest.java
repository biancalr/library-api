package br.com.libraryapi.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.libraryapi.api.dto.BookDTO;
import br.com.libraryapi.exception.BusinessException;
import br.com.libraryapi.model.entity.Book;
import br.com.libraryapi.service.BookService;
import br.com.libraryapi.service.LoanService;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@WebMvcTest(controllers = BookController.class)
@AutoConfigureMockMvc
public class BookControllerTest {

	private static String BOOK_API = "/api/books";

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private BookService service;
	
	@MockBean
	private LoanService loanService;

	@Test
	@DisplayName("Deve criar um livro com sucesso.")
	public void createBookTest() throws Exception {

		BookDTO dto = createNewBook();
		Book savedBook = Book.builder().id(10l).author("Artur").title("As aventuras").isbn("001").build();

		BDDMockito.given(service.save(Mockito.any(Book.class))).willReturn(savedBook);

		String json = new ObjectMapper().writeValueAsString(dto);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(BOOK_API)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(json);

		mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.jsonPath("id").value(10l))
				.andExpect(jsonPath("title").value(dto.getTitle())).andExpect(jsonPath("author").value(dto.getAuthor()))
				.andExpect(jsonPath("isbn").value(dto.getIsbn()));

	}

	@Test
	@DisplayName("Deve laçar erro de validação quando não houver " + "dados suficiente para criação do livro.")
	public void createInvalidBookTest() throws Exception {

		String json = new ObjectMapper().writeValueAsString(new BookDTO());

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(BOOK_API)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(json);

		mockMvc.perform(request).andExpect(status().isBadRequest()).andExpect(jsonPath("errors", Matchers.hasSize(3)));
	}

	@Test
	@DisplayName("Deve lancar erro ao tentar cadastrar um livro com um isbn ja utilizado por outro")
	public void createBookWithDuplicatedIsbn() throws Exception {

		BookDTO dto = createNewBook();
		String json = new ObjectMapper().writeValueAsString(dto);
		String mensagemErro = "ISBN já cadastrado.";
		BDDMockito.given(service.save(Mockito.any(Book.class))).willThrow(new BusinessException("ISBN já cadastrado."));

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(BOOK_API)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(json);

		mockMvc.perform(request).andExpect(status().isBadRequest()).andExpect(jsonPath("errors", hasSize(1)))
				.andExpect(jsonPath("errors[0]").value(mensagemErro));
	}

	@Test
	@DisplayName("Deve obter informacoes de um livro")
	public void getBookDetailTest() throws Exception {
		// cenario
		Long id = 1l;

		Book book = Book.builder().id(id).title(createNewBook().getTitle()).author(createNewBook().getAuthor())
				.isbn(createNewBook().getIsbn()).build();

		BDDMockito.given(service.getById(id)).willReturn(Optional.of(book));

		// execucao
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(BOOK_API.concat("/" + id))
				.accept(MediaType.APPLICATION_JSON);

		// verificacao
		mockMvc.perform(request).andExpect(status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("id").value(id))
				.andExpect(jsonPath("title").value(createNewBook().getTitle()))
				.andExpect(jsonPath("author").value(createNewBook().getAuthor()))
				.andExpect(jsonPath("isbn").value(createNewBook().getIsbn()));

	}

	@Test
	@DisplayName("Deve retornar ResourceNotFound quando o livro procurado nao existir")
	public void bookNotFoundTest() throws Exception {

		// cenario
		BDDMockito.given(service.getById(Mockito.anyLong())).willReturn(Optional.empty());

		// execucao
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(BOOK_API.concat("/" + 1))
				.accept(MediaType.APPLICATION_JSON);
		// verificacao
		mockMvc.perform(request).andExpect(status().isNotFound());

	}

	@Test
	@DisplayName("Deve deletar um livro")
	public void deleteBookTest() throws Exception {

		// cenario
		BDDMockito.given(service.getById(Mockito.anyLong())).willReturn(Optional.of(Book.builder().id(1l).build()));

		// execucao
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.delete(BOOK_API.concat("/" + 1));

		// verificacao
		mockMvc.perform(request).andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("Deve retornar ResourceNotFound quando nao encpntrar o livro para deletar")
	public void deleteInexistentBookTest() throws Exception {

		// cenario
		BDDMockito.given(service.getById(Mockito.anyLong())).willReturn(Optional.empty());

		// execucao
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.delete(BOOK_API.concat("/" + 1));

		// verificacao
		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Deve atualizar um livro")
	public void updateBookTest() throws Exception {
		// cenario
		Long id = 1l;
		String json = new ObjectMapper().writeValueAsString(createNewBook());

		Book updatingBook = Book.builder().id(1l).title("some title").author("some author").isbn("321").build();
		BDDMockito.given(service.getById(id)).willReturn(Optional.of(updatingBook));

		Book updatedBook = Book.builder().id(1l).author("Artur").title("As aventuras").isbn("321").build();
		BDDMockito.given(service.update(updatingBook)).willReturn(updatedBook);

		// execucao
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.put(BOOK_API.concat("/" + 1)).content(json)
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON);

		// verificacao
		mockMvc.perform(request).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("id").value(id))
				.andExpect(jsonPath("title").value(createNewBook().getTitle()))
				.andExpect(jsonPath("author").value(createNewBook().getAuthor()))
				.andExpect(jsonPath("isbn").value("321"));

	}

	@Test
	@DisplayName("Deve retornar ao tentar atualizar um livro")
	public void updateInexistentBookTest() throws Exception {
		// cenario
		String json = new ObjectMapper().writeValueAsString(createNewBook());
		BDDMockito.given(service.getById(Mockito.anyLong())).willReturn(Optional.empty());

		// execucao
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.put(BOOK_API.concat("/" + 1)).content(json)
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON);

		// verificacao
		mockMvc.perform(request).andExpect(status().isNotFound());

	}

	@Test
	@DisplayName("Deve filtrar livros")
	public void findBooksTest() throws Exception {

		// cenario
		Long id = 1l;
		Book book = Book.builder().id(id)
						.author(createNewBook().getAuthor())
						.title(createNewBook().getTitle())
						.isbn(createNewBook().getIsbn())
						.build();

		BDDMockito.given( service.find(Mockito.any(Book.class), Mockito.any(Pageable.class)) )
				  .willReturn( new PageImpl<Book>( Arrays.asList(book), PageRequest.of(0, 100), 1) );
		
		String queryString = String.format("?title=%s&author=%s&page=0&size=100", book.getTitle(), book.getAuthor());
		
		// execucao
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(BOOK_API.concat(queryString))
				.accept(MediaType.APPLICATION_JSON);
		
		// verificacao
		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("content", hasSize(1)))
				.andExpect(jsonPath("totalElements").value(1)).andExpect(jsonPath("pageable.pageSize").value(100))
				.andExpect(jsonPath("pageable.pageNumber").value(0));

	}

	private BookDTO createNewBook() {
		return BookDTO.builder().author("Artur").title("As aventuras").isbn("001").build();
	}

}
