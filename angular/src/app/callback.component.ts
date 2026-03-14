import { Component } from '@angular/core';

@Component({
  selector: 'app-callback',
  standalone: true,
  template: `
    <div style="padding: 20px; border: 1px solid #28a745; border-radius: 5px; margin-top: 20px; background-color: #f0fff4;">
      <h2 style="color: #28a745;">Login Successful! 🎉</h2>
      <p>You have successfully completed the OAuth2 flow and returned to the Angular frontend.</p>
      <a href="/" style="color: #007bff; text-decoration: none;">&larr; Go back Home</a>
    </div>
  `
})
export class CallbackComponent {}