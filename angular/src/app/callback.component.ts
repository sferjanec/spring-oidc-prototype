import { Component } from '@angular/core';

@Component({
  selector: 'app-callback',
  standalone: true,
  template: `
    <h2>Login Callback Successful!</h2>
    <p>You have been redirected back to the Angular application at /login/callback.</p>
  `,
})
export class CallbackComponent {}
