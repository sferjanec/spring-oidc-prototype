import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <h1>Mock Okta Angular Frontend</h1>
    <p>Host: sso-peanut.localhost</p>
    <br>
    <a href="/oauth2/authorization/okta" style="padding: 10px; background: #007bff; color: white; text-decoration: none; border-radius: 4px; display: inline-block;">Login with Okta Mock</a>
    <hr>
    <router-outlet></router-outlet>
  `
})
export class AppComponent {
  title = 'frontend';
}
