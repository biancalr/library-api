package br.com.libraryapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import br.com.libraryapi.exception.BusinessException;
import br.com.libraryapi.model.entity.Book;
import br.com.libraryapi.model.repository.BookRepository;
import br.com.libraryapi.service.impl.BookServiceImpl;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class BookServiceTest {

	private BookService service;
	
	@MockBean
	private BookRepository repository;
	
	@BeforeEach
	public void setUp() {
		this.service = new BookServiceImpl(repository);
	}
	
	@Test
	@DisplayName("Deve salvar um livro")
	public void saveBookTest() {
		// cenario
		Book book = createValidBook();
		Mockito.when(repository.existsByIsbn(Mockito.anyString())).thenReturn(false);
		Mockito.when(repository.save(book))
			.thenReturn(Book.builder()
						.id(1l)
						.isbn("123")
						.title("As aventuras")
						.author("Fulano")
						.build());
		
		//execucao
		Book savedBook = service.save(book);
		
		// verificacao
		assertThat(savedBook.getId()).isNotNull();
		assertThat(savedBook.getIsbn()).isEqualTo("123");
		assertThat(savedBook.getTitle()).isEqualTo("As aventuras");
		assertThat(savedBook.getAuthor()).isEqualTo("Fulano");
		
		
	}

	private Book createValidBook() {
		return Book.builder().isbn("123").author("Fulano")
					.title("As aventuras").build();
	}
	
	@Test
	@DisplayName("Deve lancar erro de negocio ao tentar lancar livro com ISBN duplicad")
	public void shoulNotSaveABookWithDuplicatedISBN() {
		// cenario
		Book book = createValidBook();
		Mockito.when(repository.existsByIsbn(Mockito.anyString())).thenReturn(true);
		
		//execucao
		Throwable exception = Assertions.catchThrowable(() -> service.save(book)) ;
		
		// verificacoes
		assertThat(exception)
			.isInstanceOf(BusinessException.class)
			.hasMessage("ISBN já cadastrado.");
		
		Mockito.verify(repository, Mockito.never()).save(book);
		
	}
	
	@Test
	@DisplayName("Deve obter um livro por id")
	public void getByIdTest() {
		
		//cenario
		Long id = 1l;
		Book book = createValidBook();
		book.setId(id);
		
		Mockito.when(repository.findById(id))
			   .thenReturn(Optional.of(book));
		
		//execucao
		Optional<Book> foundBook = service.getById(id);
		
		// verificacao
		assertThat(foundBook.isPresent()).isTrue();
		assertThat(foundBook.get().getId()).isEqualTo(id);
		assertThat(foundBook.get().getAuthor()).isEqualTo(book.getAuthor());
		assertThat(foundBook.get().getTitle()).isEqualTo(book.getTitle());
		assertThat(foundBook.get().getIsbn()).isEqualTo(book.getIsbn());
				
	}
	
	@Test
	@DisplayName("Deve retornar vazio ao obter um livro por id quando ele nao existe na base")
	public void bookNotFoundByIdTest() {
		
		//cenario
		Long id = 1l;
		
		Mockito.when(repository.findById(id))
			   .thenReturn(Optional.empty());
		
		//execucao
		Optional<Book> book = service.getById(id);
		
		// verificacao
		assertThat(book.isPresent()).isFalse();
	}
	
	@Test
	@DisplayName("Deve deletar um livro")
	public void deleteBookTest() {
		
		// cenario
		Long id = 1l;
		Book book = createValidBook();
		book.setId(id);
		
		// execucao
		org.junit.jupiter.api.Assertions.assertDoesNotThrow( 
				() -> service.delete(book));
		
		// verificacao
		Mockito.verify(repository, Mockito.times(1)).delete(book);
	}
	
	@Test
	@DisplayName("Deve ocorrer erro ao tentar deletar um livro inexistente")
	public void deleteInvalidBookTest() {
		
		// cenario
		Book book = new Book();
		
		// execucao
		org.junit.jupiter.api.Assertions.assertThrows(
								IllegalArgumentException.class, 
								() -> service.delete(book));
		
		// verificacao
		Mockito.verify(repository, Mockito.never()).delete(book);
	}
	
	@Test
	@DisplayName("Deve atualizar um livro")
	public void updateBookTest() {
		
		// cenario
		Long id = 1l;
		//livro a atualizar
		Book toUpdateBook = Book.builder().id(id).build();
		//simulacao
		Book updatedBook = createValidBook();
		updatedBook.setId(id);
		Mockito.when(repository.saveAndFlush(toUpdateBook))
				.thenReturn(updatedBook);
		
		// execucao
		Book book = service.update(toUpdateBook);
		
		// verificacao
		assertThat(book.getId()).isEqualTo(updatedBook.getId());
		assertThat(book.getIsbn()).isEqualTo(updatedBook.getIsbn());
		assertThat(book.getTitle()).isEqualTo(updatedBook.getTitle());
		assertThat(book.getAuthor()).isEqualTo(updatedBook.getAuthor());
	
	}
	
	@Test
	@DisplayName("Deve ocorrer erro ao atualizar um livro inexistente")
	public void updateInvalidBook() {
		// cenario
		Book book = new Book();
		
		// execucao
		org.junit.jupiter.api.Assertions.assertThrows(
								IllegalArgumentException.class, 
								() -> service.update(book));
		
		// verificacao
		Mockito.verify(repository, Mockito.never()).saveAndFlush(book);

	}
	
	@SuppressWarnings("unchecked")
	@Test
	@DisplayName("Deve filtrar livros pelas propriedades")
	public void findBookTest() {
		
		// cenario
		Book book = createValidBook();
		List<Book> list = new ArrayList<Book>();
		list.add(book);
		
		PageRequest pageRequest = PageRequest.of(0, 10);
		Page<Book> page = new PageImpl<Book>(list, pageRequest, 1);
		
		Mockito.when(repository.findAll(Mockito.any(Example.class), Mockito.any(PageRequest.class)))
			   .thenReturn(page);

		// execucao
		Page<Book> result = service.find(book, pageRequest);
		
		// verificacao
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent()).isEqualTo(list);
		assertThat(result.getPageable().getPageNumber()).isEqualTo(0);
		assertThat(result.getPageable().getPageSize()).isEqualTo(10);
		
	}
	
	@Test
	@DisplayName("Deve obter um livro pelo ISBN")
	public void getBookByIsbnTest() {
		
		// cenario
		String isbn = "1230";
		Mockito.when(repository.findByIsbn(isbn))
			   .thenReturn(Optional.of(
					   Book.builder().id(1l).isbn(isbn).build()));
		
		// execucao
		Optional<Book> book = service.getBookByIsbn(isbn);
		
		//verificacao
		assertThat(book.isPresent()).isTrue();
		assertThat(book.get().getId()).isEqualTo(1l);
		assertThat(book.get().getIsbn()).isEqualTo(isbn);
		Mockito.verify(repository, times(1)).findByIsbn(isbn);
	}
	
}
