package br.com.libraryapi.model.repository;

import static br.com.libraryapi.model.repository.BookRepositoryTest.createNewBook;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import br.com.libraryapi.model.entity.Book;
import br.com.libraryapi.model.entity.Loan;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@DataJpaTest
public class LoanRepositoryTest {

	@Autowired
	private LoanRepository repository;
	
	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("Deve verificar se existe emprestimo nao devolvido para livro")
	public void existsByBookAndNotReturnedTest() {
	
		// cenario
		Loan loan = createAndPersistLoan(LocalDate.now());
		Book book = loan.getBook();
		
		// execucao
		boolean exists = repository.existsByBookAndNotReturned(book);
		
		// verificacao
		assertThat(exists).isTrue();
	}
	
	@Test
	@DisplayName("Deve buscar emprestimo pelo isbn do livro ou customer")
	public void findByBookIsbnOrCustomerTest() {
		
		// cenario
		Loan loan = createAndPersistLoan(LocalDate.now());
		
		// execucao
		Page<Loan> result = repository.findByBookIsbnOrCustomer("123", "Fulano", PageRequest.of(0, 10));
		
		// verificacao
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent()).contains(loan);
		assertThat(result.getPageable().getPageSize()).isEqualTo(10);
		assertThat(result.getPageable().getPageNumber()).isEqualTo(0);
		assertThat(result.getTotalElements()).isEqualTo(1);
		
	} 
	
	@Test
	@DisplayName("Deve obter emprestimos cuja data de emprestimo for menor ou igual a tres dias atras e nao retornados")
	public void findByDateLessThanAndNotReturned() {
		
		// cenario
		Loan loan = createAndPersistLoan(LocalDate.now().minusDays(5));
		
		// execucao
		List<Loan> result = repository.findByLoansDateLessThanAndNotReturned(LocalDate.now().minusDays(4));
		
		// verificacao
		assertThat(result).hasSize(1).contains(loan);
		
	}
	
	@Test
	@DisplayName("Deve retornar vazio quando nao houver emprestimos atrasados")
	public void notfindByDateLessThanAndNotReturned() {
		
		// cenario
		createAndPersistLoan(LocalDate.now());
		
		// execucao
		List<Loan> result = repository.findByLoansDateLessThanAndNotReturned(LocalDate.now().minusDays(4));
		
		// verificacao
		assertThat(result).isEmpty();
		
	}
	
	public Loan createAndPersistLoan(LocalDate loanDate) {
		Book book = createNewBook("123");
		entityManager.persist(book);
		
		Loan loan = Loan.builder()
						.book(book)
						.customer("Fulano")
						.loanDate(loanDate)
						.build();
		
		entityManager.persist(loan);
		return loan;
	}
	
}
