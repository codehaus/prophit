
#ifndef __auto_array_ptr_h__
#define __auto_array_ptr_h__

#include <memory>

template<class _Ty>
	class auto_array_ptr {
public:
	typedef _Ty element_type;
	explicit auto_array_ptr(_Ty *_P = 0) _THROW0()
		: _Owns(_P != 0), _Ptr(_P) {}
	auto_array_ptr(const auto_array_ptr<_Ty>& _Y) _THROW0()
		: _Owns(_Y._Owns), _Ptr(_Y.release()) {}
	auto_array_ptr<_Ty>& operator=(const auto_array_ptr<_Ty>& _Y) _THROW0()
		{if (this != &_Y)
			{if (_Ptr != _Y.get())
				{if (_Owns)
					delete[] _Ptr;
				_Owns = _Y._Owns; }
			else if (_Y._Owns)
				_Owns = true;
			_Ptr = _Y.release(); }
		return (*this); }
	~auto_array_ptr()
		{if (_Owns)
			delete[] _Ptr; }
	_Ty& operator[](int i) const _THROW0()
		{return get()[i]; }
	_Ty& operator*() const _THROW0()
		{return (*get()); }
	_Ty *operator->() const _THROW0()
		{return (get()); }
	_Ty *get() const _THROW0()
		{return (_Ptr); }
	_Ty *release() const _THROW0()
		{((auto_ptr<_Ty> *)this)->_Owns = false;
		return (_Ptr); }
private:
	bool _Owns;
	_Ty *_Ptr;
	};

#endif 