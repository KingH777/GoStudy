import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.example.finance.model.User;
import org.example.finance.repository.UserRepository;
import org.example.finance.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User adminUser;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword("123");
        adminUser.setMonthlyExpenseLimit(2000.0);
    }

    @Test
    public void testInit_whenNoUsers_createsAdminUser() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        userService.init();

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testAuthenticate_success() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        boolean result = userService.authenticate("admin", "123");

        assertTrue(result);
    }

    @Test
    public void testAuthenticate_failure() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        boolean result = userService.authenticate("admin", "wrongpassword");

        assertFalse(result);
    }

    @Test
    public void testChangePassword_success() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        boolean result = userService.changePassword("123", "456");

        assertTrue(result);
        assertEquals("456", adminUser.getPassword());
    }

    @Test
    public void testChangePassword_failure_wrongOldPassword() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        boolean result = userService.changePassword("wrongOld", "456");

        assertFalse(result);
    }

    @Test
    public void testGetMonthlyExpenseLimit() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        Double limit = userService.getMonthlyExpenseLimit();

        assertEquals(2000.0, limit);
    }

    @Test
    public void testSetMonthlyExpenseLimit() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        userService.setMonthlyExpenseLimit(3000.0);

        assertEquals(3000.0, adminUser.getMonthlyExpenseLimit());
        verify(userRepository, times(1)).save(adminUser);
    }
}
