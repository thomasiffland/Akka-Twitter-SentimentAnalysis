import { TwitterfeedFrontendPage } from './app.po';

describe('twitterfeed-frontend App', () => {
  let page: TwitterfeedFrontendPage;

  beforeEach(() => {
    page = new TwitterfeedFrontendPage();
  });

  it('should display welcome message', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('Welcome to app!');
  });
});
