<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di\Converter\Argument\Type;

use Core\Di\Converter\Argument\TypeFactory;

/**
 * Class AbstractType
 */
abstract class AbstractType
{
    const VALUE = 'value';

    /**
     * @var TypeFactory
     */
    protected $typeFactory;

    /**
     * Constructor
     *
     * @param TypeFactory $typeFactory
     */
    public function __construct(TypeFactory $typeFactory)
    {
        $this->typeFactory = $typeFactory;
    }
}
