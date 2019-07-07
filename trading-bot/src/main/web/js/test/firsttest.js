var assert = require('assert');

describe('Array', function() {
  describe('#indexOf()', function() {
    it('should return -1 when the value is not present', function() {
      assert.equal([1, 2, 3].indexOf(4), -1);
    });
  });
});

/*
describe("distance", function() {
    it("calculates distance with the good ol' Pythagorean Theorem", function() {
      let origin = {x: 0.0, y: 0.0};
      let point = {x: 3.0, y: 4.0};
      expect(distance(point, origin)).to.equal(2071);  });
  });
*/