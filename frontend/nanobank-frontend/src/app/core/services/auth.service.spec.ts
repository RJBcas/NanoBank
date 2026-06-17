import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from './auth.service';
import { AuthResponse } from '@core/models/auth.model';

const mockUser: AuthResponse = {
  accessToken: 'jwt-token',
  tokenType: 'Bearer',
  userId: 'user-1',
  email: 'test@nanobank.com',
  fullName: 'Test User',
  role: 'ROLE_USER'
};

describe('AuthService - loadUser()', () => {
  afterEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('should restore user when valid JSON exists in localStorage', () => {
    localStorage.setItem('nb_user', JSON.stringify(mockUser));
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule]
    });
    const svc = TestBed.inject(AuthService);
    expect(svc.isLoggedIn()).toBeTrue();
    expect(svc.currentUser()?.email).toBe('test@nanobank.com');
  });

  it('should return null when localStorage contains invalid JSON', () => {
    localStorage.setItem('nb_user', '{corrupted json}');
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule]
    });
    const svc = TestBed.inject(AuthService);
    expect(svc.isLoggedIn()).toBeFalse();
  });
});

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule]
    });
    service = TestBed.inject(AuthService);
    http    = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { http.verify(); localStorage.clear(); });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('isLoggedIn should be false initially', () => {
    expect(service.isLoggedIn()).toBeFalse();
  });

  it('login() should persist user and set isLoggedIn', () => {
    service.login({ email: 'test@nanobank.com', password: 'pass' }).subscribe(res => {
      expect(res.data.accessToken).toBe('jwt-token');
    });

    const req = http.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: mockUser, message: 'ok' });

    expect(service.isLoggedIn()).toBeTrue();
    expect(service.currentUser()?.email).toBe('test@nanobank.com');
    expect(localStorage.getItem('nb_token')).toBe('jwt-token');
  });

  it('register() should persist user', () => {
    service.register({ email: 'new@test.com', password: 'pass1234', fullName: 'New User' }).subscribe();

    const req = http.expectOne('/api/v1/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: { ...mockUser, email: 'new@test.com' }, message: 'ok' });

    expect(service.isLoggedIn()).toBeTrue();
  });

  it('getToken() should return null when not logged in', () => {
    expect(service.getToken()).toBeNull();
  });

  it('logout() should clear user state', () => {
    // Simulate logged in state
    localStorage.setItem('nb_token', 'jwt-token');
    localStorage.setItem('nb_user', JSON.stringify(mockUser));

    const freshService = TestBed.inject(AuthService);
    freshService.logout();

    expect(freshService.isLoggedIn()).toBeFalse();
    expect(localStorage.getItem('nb_token')).toBeNull();
  });
});
