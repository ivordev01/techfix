package techfix.techfix.customer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class CustomerService {

	private final ConcurrentMap<String, Customer> customers = new ConcurrentHashMap<>();
	private final AtomicInteger customerCounter = new AtomicInteger(4);

	public CustomerService() {
		customers.put("CLI-001", new Customer("CLI-001", "Luana Costa", "123.456.789-00", "(11) 98765-4321",
				"Rua das Flores, 123, Centro - São Paulo/SP - 01234-567"));
		customers.put("CLI-002", new Customer("CLI-002", "Carlos Henrique", "234.567.890-11", "(11) 97654-3210",
				"Av. Paulista, 1000, Bela Vista - São Paulo/SP - 01310-100"));
		customers.put("CLI-003", new Customer("CLI-003", "Maria Silva", "345.678.901-22", "(11) 96543-2109",
				"Rua Augusta, 500, Consolação - São Paulo/SP - 01305-000"));
		customers.put("CLI-004", new Customer("CLI-004", "João Santos", "456.789.012-33", "(11) 95432-1098",
				"Rua dos Três Irmãos, 200, Butantã - São Paulo/SP - 05360-000"));
	}

	public List<Customer> findAll() {
		return new ArrayList<>(customers.values());
	}

	public List<Customer> search(String query) {
		String normalized = query.toLowerCase();
		String digits = query.replaceAll("[^0-9]", "");

		return customers.values().stream().filter(customer -> customer.id().toLowerCase().contains(normalized)
				|| customer.name().toLowerCase().contains(normalized)
				|| customer.cpf().replaceAll("[^0-9]", "").contains(digits)).toList();
	}

	public Customer create(String name, String cpf, String phone, String address) {
		boolean cpfInUse = customers.values().stream()
				.anyMatch(existing -> existing.cpf().equalsIgnoreCase(cpf));
		if (cpfInUse) {
			throw new IllegalArgumentException("Já existe cliente cadastrado com o CPF informado");
		}

		String id = "CLI-" + String.format("%03d", customerCounter.incrementAndGet());
		Customer customer = new Customer(id, name.trim(), cpf, phone.trim(), address.trim());
		customers.put(id, customer);
		return customer;
	}

	public Optional<Customer> findById(String id) {
		return Optional.ofNullable(customers.get(id));
	}

	public Customer getRequired(String id) {
		return findById(id).orElseThrow(() -> new IllegalArgumentException("Cliente com ID %s não encontrado".formatted(id)));
	}

	public record Customer(String id, String name, String cpf, String phone, String address) {
	}
}


